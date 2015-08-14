/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2005-2006, David Zwiers
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.wfs.v1_0_0;

import static org.geotools.data.wfs.protocol.http.HttpMethod.POST;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.OperationNotSupportedException;

import org.geotools.data.Transaction;
import org.geotools.data.Transaction.State;
import org.geotools.data.wfs.v1_0_0.Action.DeleteAction;
import org.geotools.data.wfs.v1_0_0.Action.InsertAction;
import org.geotools.data.wfs.v1_0_0.Action.UpdateAction;
import org.geotools.data.wfs.v1_0_0.xml.WFSSchema;
import org.geotools.util.logging.Logging;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.DocumentWriter;
import org.geotools.xml.SchemaFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.xml.sax.SAXException;

/**
 * Hold the list of actions to perform in the Transaction.
 * 
 * @author dzwiers
 *
 *
 *
 * @source $URL$
 */
public class WFSTransactionState implements State {
    private WFS_1_0_0_DataStore ds = null;

    /**
     * A map of <String, String[]>. String is the typename and String[] are the
     * fids returned by Transaction Results during the last commit.
     * <p>
     * They are the fids of the inserted elements.
     */
    private Map<String, String[]> fids = new HashMap<String, String[]>();

    /**
     * A Map of <String, List<Action>> where string is the typeName of the
     * feature type and the list is the list of actions that have modified the
     * feature type
     */
    Map<String, List<Action>> actionMap = new HashMap<String, List<Action>>();

    private long latestFid = Long.MAX_VALUE;

    private Transaction transaction;

    /** Private - should not be used */
    @SuppressWarnings("unused")
    private WFSTransactionState() {
        // subclass must supply ds
    }

    /**
     * @param ds
     */
    public WFSTransactionState(WFS_1_0_0_DataStore ds) {
        this.ds = ds;
    }

    /**
     * @see org.geotools.data.Transaction.State#setTransaction(org.geotools.data.Transaction)
     */
    public void setTransaction(Transaction transaction) {
        if (transaction != null) {
            synchronized (actionMap) {
                this.transaction = transaction;
                synchronized (fids) {
                    fids.clear();
                }
                actionMap.clear();
            }
        }
    }

    /**
     * @see org.geotools.data.Transaction.State#addAuthorization(java.lang.String)
     */
    public void addAuthorization(String AuthID) {
        // authId = AuthID;
    }

    /**
     * Not implemented
     * 
     * @return String
     */
    public String getLockId() {
        return null; // add this later
    }

    /**
     * @see org.geotools.data.Transaction.State#commit()
     */
    public void commit() throws IOException {
        // TODO deal with authID and locking ... WFS only allows one authID /
        // transaction ...
        TransactionResult transactionResult = null;

        Map<String, List<Action>> copiedActions;
        synchronized (actionMap) {
            combineActions();
            copiedActions = copy(actionMap);
        }
        Iterator<Entry<String, List<Action>>> iter = copiedActions.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, List<Action>> entry = iter.next();
            List<Action> actions = entry.getValue();
            String typeName = (String) entry.getKey();

            if (actions.isEmpty()){
                continue;
            }

            if (transactionResult == null) {
                try {
                    transactionResult = commitPost(actions);
                } catch (OperationNotSupportedException e) {
                    WFS_1_0_0_DataStore.LOGGER.warning(e.toString());
                    transactionResult = null;
                } catch (SAXException e) {
                    WFS_1_0_0_DataStore.LOGGER.warning(e.toString());
                    transactionResult = null;
                }
            }

            if (transactionResult == null) {
                throw new IOException("An error occured while committing.");
            }

            if (transactionResult.getStatus() == TransactionResult.FAILED) {
                throw new IOException(transactionResult.getError().toString());
            }

            List<String> newFids = transactionResult.getInsertResult();
            int currentInsertIndex = 0;
            for (Iterator<Action> iter2 = actions.iterator(); iter2.hasNext();) {
                Action action = iter2.next();
                if (action instanceof InsertAction) {
                    InsertAction insertAction = (InsertAction) action;
                    if (currentInsertIndex >= newFids.size()) {
                        Logging.getLogger("org.geotools.data.wfs").severe(
                                "Expected more fids to be returned by " + "TransactionResponse!");
                        break;
                    }
                    String tempFid = insertAction.getFeature().getID();
                    String finalFid = newFids.get(currentInsertIndex);
                    
                    ds.addFidMapping(tempFid, finalFid);
                    currentInsertIndex++;
                }
            }
            synchronized (this.fids) {
                this.fids.put(typeName, (String[]) newFids.toArray(new String[0]));
            }
            if (currentInsertIndex != newFids.size()) {
                Logging.getLogger("org.geotools.data.wfs").severe(
                        "number of fids inserted do not match number of fids returned "
                                + "by Transaction Response.  Got:" + newFids.size() + " expected: "
                                + currentInsertIndex);
            }
            synchronized (actionMap) {
                ((List<Action>) actionMap.get(typeName)).removeAll(actions);
            }
        }
    }

    private Map<String, List<Action>> copy(Map<String, List<Action>> actionMap2) {
        Map<String, List<Action>> newMap = new HashMap<String, List<Action>>();
        Iterator<Entry<String, List<Action>>> entries = actionMap2.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, List<Action>> entry = entries.next();
            List<Action> list =  entry.getValue();
            newMap.put(entry.getKey(), new ArrayList<Action>(list));
        }
        return newMap;
    }

    private TransactionResult commitPost(List<Action> toCommit) throws OperationNotSupportedException,
            IOException, SAXException {
        
        URL postUrl = ds.capabilities.getTransaction().getPost();
        // System.out.println("POST Commit URL = "+postUrl);
        if (postUrl == null) {
            throw new UnsupportedOperationException("Capabilities document does not describe a valid POST url for Transaction");
            //return null;
        }

        HttpURLConnection hc = ds.protocolHandler.getConnectionFactory().getConnection(postUrl,
                POST);
        // System.out.println("connection to commit");
        Map<String,Object> hints = new HashMap<String,Object>();
        hints.put(DocumentWriter.BASE_ELEMENT, WFSSchema.getInstance().getElements()[24]); // Transaction
        hints.put(DocumentWriter.ENCODING, ds.getDefaultEncoding());
        
        Set<String> fts = new HashSet<String>();
        Iterator<Action> i = toCommit.iterator();
        while (i.hasNext()) {
            Action a = (Action) i.next();
            fts.add(a.getTypeName());
        }
        Set<String> ns = new HashSet<String>();
        ns.add(WFSSchema.NAMESPACE.toString());
        Iterator<String> i2 = fts.iterator();
        while (i2.hasNext()) {
            String target = (String) i2.next();
            SimpleFeatureType schema = ds.getSchema(target);
            
            try {
                String namespaceURI = schema.getName().getNamespaceURI();
                ns.add(namespaceURI);
                URI namespaceLocation = ds.getDescribeFeatureTypeURL(target).toURI();
                // if this is not added then sometimes the schema for the describe feature type cannot be loaded and
                // an exception will be thrown during the commit 
                SchemaFactory.getInstance(new URI(namespaceURI), namespaceLocation);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        hints.put(DocumentWriter.SCHEMA_ORDER, ns.toArray(new String[ns.size()])); // Transaction

        // System.out.println("Ready to print Debug");
        // // DEBUG
        // StringWriter debugw = new StringWriter();
        // DocumentWriter.writeDocument(this, WFSSchema.getInstance(), debugw,
        // hints);
        // System.out.println("TRANSACTION \n\n");
        // System.out.println(debugw.getBuffer());
        // // END DEBUG

        OutputStream os = hc.getOutputStream();

        // write request
        final String encoding = ds.getDefaultEncoding();
        Writer w = new OutputStreamWriter(os, encoding);
        Logger logger = Logging.getLogger("org.geotools.data.wfs");
        if (logger.isLoggable(Level.FINE)) {
            w = new LogWriterDecorator(w, logger, Level.FINE);
        }
        // special logger for communication information only.
        logger = Logging.getLogger("org.geotools.data.communication");
        if (logger.isLoggable(Level.FINE)) {
            w = new LogWriterDecorator(w, logger, Level.FINE);
        }

        if(transaction != null && transaction.getProperty("handle") instanceof String){
            String commitMessageHandle = (String) transaction.getProperty("handle");
            if(commitMessageHandle != null){
                hints.put("handle", commitMessageHandle);
            }
        }
        
        DocumentWriter.writeDocument(this, WFSSchema.getInstance(), w, hints);
        w.flush();
        w.close();

        InputStream is = this.ds.protocolHandler.getConnectionFactory().getInputStream(hc);

        hints = new HashMap<String,Object>();

        TransactionResult ft = (TransactionResult) DocumentFactory.getInstance(is, hints,
                Level.WARNING);
        return ft;
    }

    /**
     * @see org.geotools.data.Transaction.State#rollback()
     */
    public void rollback() {
        synchronized (actionMap) {
            actionMap.clear();
        }
    }

    /**
     * @return Fid Set
     */
    public String[] getFids(String typeName) {
        synchronized (fids) {
            return fids.get(typeName);
        }
    }

    /**
     * @param a
     */
    public void addAction(String typeName, Action a) {
        synchronized (actionMap) {
            List<Action> list =  actionMap.get(typeName);
            if (list == null) {
                list = new ArrayList<Action>();
                actionMap.put(typeName, list);
            }
            list.add(a);
        }
    }

    /**
     * @return List of Actions
     */
    public List<Action> getActions(String typeName) {
        synchronized (actionMap) {
            Collection<Action> collection = (Collection<Action>) actionMap.get(typeName);
            if (collection == null || collection.isEmpty())
                return new ArrayList<Action>();
            return new ArrayList<Action>(collection);
        }
    }

    /**
     * Returns all the actions for all FeatureTypes
     * 
     * @return all the actions for all FeatureTypes
     */
    public List<Action> getAllActions() {
        synchronized (actionMap) {
            List<Action> all = new ArrayList<Action>();
            for (Iterator<List<Action>> iter = actionMap.values().iterator(); iter.hasNext();) {
                List<Action> actions = (List<Action>) iter.next();
                all.addAll(actions);
            }
            return all;
        }
    }

    /**
     * Combines updates and inserts reducing the number of actions in the
     * commit.
     * <p>
     * This is in response to an issue where the FID is not known until after
     * the commit so if a Feature is inserted then later updated(using a FID
     * filter to identify the feature to update) within a single transactin then
     * the commit will fail because the fid filter will be not apply once the
     * insert action is processed.
     * </p>
     * <p>
     * For Example:
     * <ol>
     * <li>Insert Feature.
     * <p>
     * Transaction assigns it the id: NewFeature.
     * </p>
     * </li>
     * <li>Update Feature.
     * <p>
     * Fid filter is used to update NewFeature.
     * </p>
     * </li>
     * <li>Commit.
     * <p>
     * Update will fail because when the Insert action is processed NewFeature
     * will not refer to any feature.
     * </p>
     * </li>
     * </ol>
     * </p>
     * <p>
     * The algorithm is essentially foreach( insertAction ){ Apply each update
     * and Delete action that applies to the inserted feature move insertAction
     * to end of list }
     * </p>
     * <p>
     * Mind you this only works assuming there aren't any direct dependencies
     * between the actions beyond the ones specified by the API. For example if
     * the value of an update depends directly on an earlier feature object
     * (which is bad practice and should never be done). Then we may have
     * problems with this solution. But I think that this solution is better
     * than doing nothing because at least in the proper use of the API the
     * correct result will be obtained. Whereas before the correct use of the
     * API could obtain incorrect results.
     * </p>
     */
    protected void combineActions() {
        EACH_FEATURE_TYPE: for (Iterator<List<Action>> iter = actionMap.values().iterator(); iter.hasNext();) {
            List<Action> actions = iter.next();

            removeFilterAllActions(actions);
            InsertAction firstAction = null;
            while (firstAction == null || !actions.contains(firstAction)) {
                firstAction = findFirstInsertAction(actions);
                if (firstAction == null)
                    continue EACH_FEATURE_TYPE;
                processInsertAction(actions, firstAction);
            }
            InsertAction current = findFirstInsertAction(actions);
            while (current != null && firstAction != current) {
                processInsertAction(actions, current);
                current = findFirstInsertAction(actions);
            }
        }
    }

    /**
     * Removes all actions whose filter is Filter.EXCLUDE
     */
    private void removeFilterAllActions(List<Action> actions) {
        for (Iterator<Action> iter = actions.iterator(); iter.hasNext();) {
            Action element = (Action) iter.next();
            Filter filter = element.getFilter();

            if (Filter.EXCLUDE.equals(filter)) {
                iter.remove();
            }
        }
    }

    private InsertAction findFirstInsertAction(List<Action> actions) {
        for (Action action :  actions) {
            if (action instanceof InsertAction) {
                return (InsertAction) action;
            }
        }
        return null;
    }

    private void processInsertAction(List<Action> actions, InsertAction action) {
        int indexOf = actions.indexOf(action);
        while (indexOf + 1 < actions.size() && indexOf != -1) {
            moveUpdateAndMoveInsertAction(actions, indexOf, action);
            indexOf = actions.indexOf(action);
        }
    }

    private void moveUpdateAndMoveInsertAction(List<Action> actions, int i, InsertAction action) {
        if (i + 1 < actions.size()) {
            Object nextAction = actions.get(i + 1);
            if (nextAction instanceof DeleteAction) {
                handleDeleteAction(actions, i, action, (DeleteAction) nextAction);
            } else if (nextAction instanceof UpdateAction) {
                handleUpdateAction(actions, i, action, (UpdateAction) nextAction);
            } else
                swap(actions, i);
        }
    }

    private void handleDeleteAction(List<Action> actions, int i, InsertAction action,
            DeleteAction deleteAction) {
        // if inserted action has been deleted then remove action
        if (deleteAction.getFilter().evaluate(action.getFeature())) {
            actions.remove(i);
            // if filter is a fid filter of size 1 then it only contains the
            // inserted feature which
            // no longer exists since it has been deleted. so remove that action
            // as well.
            if (deleteAction.getFilter() instanceof Id
                    && ((Id) deleteAction.getFilter()).getIdentifiers().size() == 1) {
                actions.remove(i);
            }
        } else {
            swap(actions, i);
        }
    }

    private int handleUpdateAction(List<Action> actions, int i, InsertAction action,
            UpdateAction updateAction) {
        // if update action applies to feature then update feature
        if (updateAction.getFilter().evaluate(action.getFeature())) {
            updateAction.update(action.getFeature());
            // if filter is a fid filter and there is only 1 fid then filter
            // uniquely identifies
            // only the
            // one features so remove it.
            if (updateAction.getFilter() instanceof Id
                    && ((Id) updateAction.getFilter()).getIdentifiers().size() == 1) {
                actions.remove(i + 1);
                return i;
            }
        }
        swap(actions, i);
        return i + 1;
    }

    /**
     * swaps the action at location i with the item at location i+1
     * 
     * @param i
     *            item to swap
     */
    private void swap(List<Action> actions, int i) {
        Action item = actions.remove(i);
        actions.add(i + 1, item);
    }

    public String nextFid(String typeName) {
        long fid;
        synchronized (this) {
            fid = latestFid;
            latestFid--;
        }
        return "new" + typeName + "." + fid;
    }

}
