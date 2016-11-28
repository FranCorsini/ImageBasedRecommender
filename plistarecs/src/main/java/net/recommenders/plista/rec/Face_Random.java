package net.recommenders.plista.rec;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Math;
import java.lang.Exception;
import java.io.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.log.DataLogger;
import net.recommenders.plista.recommender.Recommender;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * CategoryBased recommender with popularity and recency information
 *
 * @author alejandr
 */
public class Face_Random implements Recommender {

    private static DataLogger logger = DataLogger.getLogger(Face_Random.class);
    protected Set<Long> forbiddenItems;
    protected PathRecommender.WeightedItemList allItems;
    protected Map<Long, Map<Long, PathRecommender.WeightedItemList>> mapDomainCategoryItems;
    protected Map<Long, PathRecommender.WeightedItemList> mapDomainItems;
    //protected Map<Long, PathRecommender.WeightedItem> allItems; 
    protected PrintWriter out = null;
    protected PrintWriter out_update = null;

    public Face_Random() {
        mapDomainCategoryItems = new ConcurrentHashMap<Long, Map<Long, PathRecommender.WeightedItemList>>();
        mapDomainItems = new ConcurrentHashMap<Long, PathRecommender.WeightedItemList>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        allItems = new PathRecommender.WeightedItemList();
        try{
            out = new PrintWriter(new BufferedWriter(new FileWriter("recommendetions.log", true)));
            out.println("INITIALIZING...");
            out_update = new PrintWriter(new BufferedWriter(new FileWriter("updates_debug.log", true)));
            out_update.println("INITIALIZING...");
        }catch (IOException e) {
            System.out.println(e);
        }
    }

    public synchronized List<Long> recommend(Message input, Integer limit) {
        final List<Long> recList = new ArrayList<Long>();

        Long domain = input.getDomainID();
        Long category = input.getItemCategory();
        int face_recs = 0;
        int norm_recs = 0;
        PathRecommender.WeightedItemList scoreList = new PathRecommender.WeightedItemList();
        final Set<Long> recItems = new HashSet<Long>();
        if (domain != null) {
            Long item = input.getItemID();
            if (item != null) {
                recItems.add(item);
                if (category != null) {
                    Map<Long, PathRecommender.WeightedItemList> categoryItems = mapDomainCategoryItems.get(domain);
                    if (categoryItems != null) {
                        PathRecommender.WeightedItemList candidateItems = categoryItems.get(category);
                        if (candidateItems != null) {

                            
                            int n = 0;
                            int temp = 0; //for images
                            int i = 0; //for images
                            int sizeC = 11; 

                            int size = Math.min(limit, candidateItems.size());

                            while (temp < size) {
                                temp ++;
                                int random = ThreadLocalRandom.current().nextInt(0, candidateItems.size() - 1);
                                PathRecommender.WeightedItem candidate = candidateItems.get(random);
                                if (forbiddenItems.contains(candidate.getItemId()) || item == candidate.getItemId() || recList.contains(candidate)) {
                                    continue; // ignore this item
                                }                                
                                try {
                                    long id = candidate.getItemId();
                                    Integer faces = candidate.getFaces(); 
                                    Integer saliency = candidate.getSal();
                                    if ( faces > 0 || saliency == 1 ){
                                        recList.add(id);
                                        recItems.add(id);
                                        scoreList.add(candidate);
                                        n++;
                                    }
                                } catch(NullPointerException e){ 
                                    System.out.println("EXCEPTION!!!");
                                    System.out.println(e);
                                }

                            }
                            //if there are no images try other categories 
                            if (n ==0){ 
                                for (Map.Entry<Long, PathRecommender.WeightedItemList> entry : categoryItems.entrySet())
                                {
                                    if (n >= size){
                                        break;
                                    }
                                    candidateItems = entry.getValue();
                                    temp = 0;
                                    while (n < size && temp < size){
                                        temp ++;
                                        int random = ThreadLocalRandom.current().nextInt(0, candidateItems.size() - 1);
                                        PathRecommender.WeightedItem candidate = candidateItems.get(random);
                                        if (forbiddenItems.contains(candidate.getItemId()) || item == candidate.getItemId() || recList.contains(candidate)) {
                                            continue; // ignore this item
                                        }                                
                                        try {
                                            long id = candidate.getItemId();
                                            Integer faces = candidate.getFaces(); 
                                            Integer saliency = candidate.getSal();
                                            if ( faces > 0 || saliency == 1 ){
                                                recList.add(id);
                                                recItems.add(id);
                                                scoreList.add(candidate);
                                                n++;
                                            }
                                        } catch(NullPointerException e){ 
                                            System.out.println("EXCEPTION!!!");
                                            System.out.println(e);
                                        }

                                    }
                                }


                            }
                            //if we are still missing recs
                            candidateItems = categoryItems.get(category);
                            while(n < size){
                                int random = ThreadLocalRandom.current().nextInt(0, candidateItems.size() - 1);
                                PathRecommender.WeightedItem candidate = candidateItems.get(random);
                                if (forbiddenItems.contains(candidate.getItemId()) || item == candidate.getItemId() || recList.contains(candidate)) {
                                    continue; // ignore this item
                                }                                
                                long id = candidate.getItemId();
                                recList.add(id);
                                recItems.add(id);
                                scoreList.add(candidate);
                                n++;
                                
                            }


                        }
                    }
                }
            }
            completeList(recList, recItems, mapDomainItems.get(domain), limit - recList.size(), forbiddenItems);
        }
        // just in case no information is found... (useful for the test case)
        completeList(recList, recItems, allItems, limit - recList.size(), forbiddenItems);
        
        //new snippet: log list for testing pourpose
        try{
            String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
            out.println("New rec on: " + timeStamp + ", Faces recs: " + face_recs + ", Norm recs: " + norm_recs + ", Domain: " + input.getDomainID());
            for (PathRecommender.WeightedItem elem : scoreList){
                out.println(elem.toString());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        //end new snippetasd
        return recList;
    }
    /*Outdated
    //@Overload New
    protected static void completeList(List<Long> recList, Set<Long> itemsAlreadyRecommended, Map<Long, PathRecommender.WeightedItem> domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        if (domainItems != null) {
            Iterator<Map.Entry<Long, PathRecommender.WeightedItem>> it = domainItems.entrySet().iterator();
            while (it.hasNext()){
                PathRecommender.WeightedItem item = it.next().getValue();
                if (n >= howMany) {
                    break;
                }
                if (!forbiddenItems.contains(item.getItemId()) && !itemsAlreadyRecommended.contains(item.getItemId())) {
                    recList.add(item.getItemId());
                    itemsAlreadyRecommended.add(item.getItemId());
                    n++;
                }
                it.remove();
            }
        }
    }*/
    //@Overload Old
    protected static void completeList(List<Long> recList, Set<Long> itemsAlreadyRecommended, PathRecommender.WeightedItemList domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        if (domainItems != null) {
            for (PathRecommender.WeightedItem item : domainItems) {
                if (n >= howMany) {
                    break;
                }
                if (!forbiddenItems.contains(item.getItemId()) && !itemsAlreadyRecommended.contains(item.getItemId())) {
                    recList.add(item.getItemId());
                    itemsAlreadyRecommended.add(item.getItemId());
                    n++;
                }
            }
        }
    }

    public void init() {
    }

    public void update(Message _update) {
        Long domainId = _update.getDomainID();
        Long item = _update.getItemID();
        Long category = _update.getItemCategory();
        Boolean recommendable = _update.getItemRecommendable();
        String url = _update.getItemURL();
        String image_url = null;
        image_url = _update.getImageURL();
        //if it doesn't exists add it
        /*
        if (allItems.containsKey(item)){ //it already exists, do not compute again
            update(domainId, item, category, recommendable, 1);
        }
        else{
            if ((domainId != null) && (item != null)) {
                update(domainId, item, category, recommendable, url ,image_url,1);
            }
        }*/


        if ((domainId != null) && (item != null)) {
            //only for debugging pourpose
            
            String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
            
            if ((url != null) && (image_url != null)){
                out_update.println("FIRST: Update on: " + timeStamp + ", Domain: " + domainId + ", Category: " + category + ", Id_Item: " + item + ", url: " + url + ", img: " + image_url);
            } else if (url != null){
                out_update.println("FIRST: Update on: " + timeStamp + ", Domain: " + domainId + ", Category: " + category + ", Id_Item: " + item + ", url: " + url );
            } else {
                out_update.println("FIRST: Update on: " + timeStamp + ", Domain: " + domainId + ", Category: " + category + ", Id_Item: " + item);
            }
            
            //finished debugging

            update(domainId, item, category, recommendable, url ,image_url,1);
        }
    }

    public void impression(Message _impression) {
        update(_impression);

        Long domainId = _impression.getDomainID();
        Long item = _impression.getItemID();
        Long category = _impression.getItemCategory();
        Boolean recommendable = _impression.getItemRecommendable();


        update(domainId, item, category, recommendable ,null, null ,1);
    }
    
    //@Overload 7 (used for real updates)
    private void update(Long domainId, Long itemId, Long category, Boolean recommendable, String url ,String image_url,int confidence) {
        //String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
        //out_update.println("SECOND: Update on: " + timeStamp + ", Id_Item: " + itemId);

        long curTime = System.currentTimeMillis();
        PathRecommender.WeightedItem newItem = new PathRecommender.WeightedItem(itemId, curTime, url, image_url);
        if (category != null) {
            Map<Long, PathRecommender.WeightedItemList> categoryItems = mapDomainCategoryItems.get(domainId);
            if (categoryItems == null) {
                categoryItems = new ConcurrentHashMap<Long, PathRecommender.WeightedItemList>();
                mapDomainCategoryItems.put(domainId, categoryItems);
            }
            PathRecommender.WeightedItemList items = categoryItems.get(category);
            if (items == null) {
                items = new PathRecommender.WeightedItemList();
                categoryItems.put(category, items);
            }
            items.add(newItem, confidence);
        }
        // without category constraint
        PathRecommender.WeightedItemList items = mapDomainItems.get(domainId);
        if (items == null) {
            items = new PathRecommender.WeightedItemList();
            mapDomainItems.put(domainId, items);
        }
        items.add(newItem, confidence);
        // all items
        allItems.add(newItem, confidence);
        if (recommendable != null && !recommendable.booleanValue()) {
            forbiddenItems.add(itemId);
        }
    }



    
    //@Overload 5 (used for click and impression)
    //protected void update(Long domainId, Long itemId, Long category, Boolean recommendable, int confidence) {
    //    long curTime = System.currentTimeMillis();
 
        /*
        //here find if the face is already found and update the confidence
        if (ItemsWithFace.containsKey(itemId)){ //depriciated as ItemsWithFace is a different hashmap now
            if (ItemsWithFace.get(itemId) == 1){
                //it arrives: 1 for impresssion, 3 for click from, 5 click to 
                confidence = confidence * 2;
            }
            else{
                confidence = Math.round(((float)confidence/2)) + confidence;
            }
        }*/
  /*
        if (category != null) {
            Map<Long, PathRecommender.WeightedItemList> categoryItems = mapDomainCategoryItems.get(domainId);
            if (categoryItems == null) {
                categoryItems = new ConcurrentHashMap<Long, PathRecommender.WeightedItemList>();
                mapDomainCategoryItems.put(domainId, categoryItems);
            }
            PathRecommender.WeightedItemList items = categoryItems.get(category);
            if (items == null) {
                items = new PathRecommender.WeightedItemList();
                categoryItems.put(category, items);
            }
            items.add(new PathRecommender.WeightedItem(itemId, curTime), confidence);
        }
        // without category constraint
        PathRecommender.WeightedItemList items = mapDomainItems.get(domainId);
        if (items == null) {
            items = new PathRecommender.WeightedItemList();
            mapDomainItems.put(domainId, items);
        }
        items.add(new PathRecommender.WeightedItem(itemId, curTime), confidence);
        // all items
        allItems.add(new PathRecommender.WeightedItem(itemId, curTime), confidence); //the creation of the item has been moved to the first update
        if (recommendable != null && !recommendable.booleanValue()) {
            forbiddenItems.add(itemId);
        }
    }*/

    public void click(Message _feedback) {
        Long domainId = _feedback.getDomainID();
        Long source = _feedback.getItemSourceID();
        Long target = _feedback.getItemID();
        Long category = _feedback.getItemCategory();
        
        if ((domainId != null) && (source != null)) {
            update(domainId, source, category, null, null, null, 3);
        }
        if ((domainId != null) && (target != null)) {
            update(domainId, target, category, null, null, null ,5);
        }

    }
    /*
    public synchronized void set_face(Long itemId,Integer number_faces) {
        PathRecommender.WeightedItem item = allItems.get(itemId);
        item.setFaces(number_faces);
        allItems.put(itemId,item);
    }*/
    public void setProperties(Properties properties) {
    }
    
    
    
}
