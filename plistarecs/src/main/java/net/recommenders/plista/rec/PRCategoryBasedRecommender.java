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

/**
 *
 * CategoryBased recommender with popularity and recency information
 *
 * @author alejandr
 */
public class PRCategoryBasedRecommender implements Recommender {

    private static DataLogger logger = DataLogger.getLogger(PRCategoryBasedRecommender.class);
    protected Set<Long> forbiddenItems;
    protected PathRecommender.WeightedItemList allItems;
    protected Map<Long, Map<Long, PathRecommender.WeightedItemList>> mapDomainCategoryItems;
    protected Map<Long, PathRecommender.WeightedItemList> mapDomainItems;
    //protected Map<Long, PathRecommender.WeightedItem> allItems; 
    protected PrintWriter out = null;
    protected PrintWriter out_update = null;
    protected PrintWriter ranked_list = null;
    protected PrintWriter new_debug_update = null;
    protected PrintWriter new_nocategory_rec = null;
    protected PrintWriter new_nocategory_up = null;
    private Object lock1 = new Object();
    private Object lock2 = new Object();

    public PRCategoryBasedRecommender() {
        mapDomainCategoryItems = new ConcurrentHashMap<Long, Map<Long, PathRecommender.WeightedItemList>>();
        mapDomainItems = new ConcurrentHashMap<Long, PathRecommender.WeightedItemList>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        allItems = new PathRecommender.WeightedItemList();
        try{
            out = new PrintWriter(new BufferedWriter(new FileWriter("recommendetions.log", true)));
            out.println("INITIALIZING...");
            out_update = new PrintWriter(new BufferedWriter(new FileWriter("updates_debug.log", true)));
            out_update.println("INITIALIZING...");
            ranked_list = new PrintWriter(new BufferedWriter(new FileWriter("ranked_list.log", true)));
            ranked_list.println("INITIALIZING...");
            new_debug_update = new PrintWriter(new BufferedWriter(new FileWriter("new_debug_update.log", true)));
            new_debug_update.println("INITIALIZING...");
            new_nocategory_rec = new PrintWriter(new BufferedWriter(new FileWriter("new_nocategory_rec.log", true)));
            new_nocategory_rec.println("INITIALIZING...");
            new_nocategory_up = new PrintWriter(new BufferedWriter(new FileWriter("new_nocategory_up.log", true)));
            new_nocategory_up.println("INITIALIZING...");
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
                            // sort it
                            Collections.sort(candidateItems);
                            
                            //for debug to see if it goes in other categories
                            Boolean otherCat = false;


                            //new snippet: first i add all elem with the face or the sal
                            int i = 0;
                            int sizeC = 11; 
                            int size = Math.min(limit, candidateItems.size());
                            for (PathRecommender.WeightedItem face_candidate : candidateItems){
                                if (i >= size || i >= sizeC) {
                                    break;
                                }
                                if (forbiddenItems.contains(face_candidate.getItemId()) || item == face_candidate.getItemId()) {
                                    continue; // ignore this item
                                }
                                long id = face_candidate.getItemId();
                                try {
                                    Integer faces = face_candidate.getFaces(); 
                                    Integer saliency = face_candidate.getSal();
                                    if ( (faces > 0) || (saliency == 1) ){
                                        recList.add(id);
                                        recItems.add(id);
                                        i++;
                                        face_recs++;
                                        scoreList.add(face_candidate);
                                    }
                                } catch(NullPointerException e){ 
                                    System.out.println("EXCEPTION!!!");
                                    System.out.println(e);
                                } //if has no face it throuws the exeption
                            }
                            //if the category has no interesting images look into other categories 
                            //this is done due to a domain which send all the recs req under a single category which has no images 
                            if (i ==0){ 
                                for (Map.Entry<Long, PathRecommender.WeightedItemList> entry : categoryItems.entrySet())
                                {
                                    candidateItems = entry.getValue();
                                    for (PathRecommender.WeightedItem face_candidate : candidateItems){
                                        if (i >= size || i >= sizeC) {
                                            break;
                                        }
                                        if (forbiddenItems.contains(face_candidate.getItemId()) || item == face_candidate.getItemId()) {
                                            continue; // ignore this item
                                        }
                                        long id = face_candidate.getItemId();
                                        //new_nocategory_rec.println("Trying");
                                        if(face_candidate.getFaces() != 0){
                                            //new_nocategory_rec.println("Got it" );
                                        }
                                        if(face_candidate.getSal() != 0){
                                            //new_nocategory_rec.println("Got it" );
                                        }
                                        try {
                                            Integer faces = face_candidate.getFaces(); 
                                            Integer saliency = face_candidate.getSal();
                                            if ( faces > 0 || saliency == 1 ){
                                                recList.add(id);
                                                recItems.add(id);
                                                i++;
                                                face_recs++;
                                                scoreList.add(face_candidate);
                                                //new_nocategory_rec.println("Got it");
                                            }
                                        } catch(NullPointerException e){ 
                                            //new_nocategory_rec.println("Exception haha");
                                            System.out.println("EXCEPTION!!!");
                                            System.out.println(e);
                                        } //if has no face it throuws the exeption
                                        otherCat = true;
                                    }

                                }


                            }

                            //end new snippet: next i add based on recency/popularity all the other elements

                            
                            int n = 0;
                            size = Math.min(limit, candidateItems.size()) - i;
                            for (PathRecommender.WeightedItem candidate : candidateItems) {
                                if (n >= size) {
                                    break;
                                }
                                if (forbiddenItems.contains(candidate.getItemId()) || item == candidate.getItemId() || recList.contains(candidate)) {
                                    continue; // ignore this item
                                }
                                recList.add(candidate.getItemId());
                                recItems.add(candidate.getItemId());
                                n++;
                                norm_recs++;
                                scoreList.add(candidate);
                            }

                            //only for debug pourpose
                            String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
                            size = candidateItems.size();
                            int counter = 0;
                            int imgcounter = 0;
                            int facesalcounter = 0;
                            synchronized(lock2){
                                //ranked_list.println("**************" + timeStamp + ", Domain: " + domain + ", Category:" + category + "*************");
                                if (otherCat){
                                    //ranked_list.println("Other Category");
                                }
                                for (PathRecommender.WeightedItem candidate : candidateItems) {
                                        if (candidate.getImg_Url() != null){
                                            //ranked_list.println("img_url:" + candidate.getImg_Url());
                                            imgcounter ++;
                                        }
                                        if (candidate.getFaces() != 0 || candidate.getSal() != 0){
                                            facesalcounter ++;
                                            //ranked_list.println("ID:" + candidate.getItemId() + ", Domain:" + input.getDomainID() + ", Freq:"+ candidate.getFreq() + ", Faces:" + candidate.getFaces() + ", Sal: " + candidate.getSal() );
                                        }
                                    
                                    
                                    counter ++;
                                    if (counter >= size){
                                        break;
                                    }
                                 }
                                //ranked_list.println("Items:" + size + ", Img:" + imgcounter +", SalFace:" + facesalcounter);
                        
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
            synchronized(lock1){
                out.println("New rec on: " + timeStamp + ", Faces recs: " + face_recs + ", Norm recs: " + norm_recs + ", Domain: " + input.getDomainID() + ", Category:" + category);
                for (PathRecommender.WeightedItem elem : scoreList){
                    out.println(elem.toString());
                }
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
            /*
            String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
            
            if ((url != null) && (image_url != null)){
                out_update.println("FIRST: Update on: " + timeStamp + ", Domain: " + domainId + ", Category: " + category + ", Id_Item: " + item + ", url: " + url + ", img: " + image_url);
            } else if (url != null){
                out_update.println("Update1");
            } else {
                out_update.println("Update2");
            }*/
            
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

        //debug pourpose

        Boolean isUpdate = true;
        try{

            if(url == null && image_url == null){
                isUpdate = false;
            }
            else{
                String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
                //new_debug_update.println("************" + timeStamp + " " + "Domain: " + domainId  + " **************");
                //new_debug_update.println("Item: " + itemId + ", Faces:"+newItem.getFaces() + ", Sal:" + newItem.getSal() + ", Category:" + category);

            }
        } catch (Exception e) {
                new_debug_update.println(e);
        }
        //

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

            //debug pourpose
            try{
                if(isUpdate){
                    //new_debug_update.println("Ranking");
                    int counter = 0;
                    int salface = 0;
                    int images = 0;
                    for (PathRecommender.WeightedItem item : items){
                        counter ++;
                        if (item.getImg_Url() != null){
                            //new_debug_update.println("img_url:" + item.getImg_Url());
                            images ++;
                        }
                        if (item.getFaces() != 0 || item.getSal() != 0){
                            salface ++;
                            //new_debug_update.println("ID:" + item.getItemId() + ", Freq:"+ item.getFreq() + ", Faces:" + item.getFaces() + ", Sal: " + item.getSal() );
                        }
                    }
                    //new_debug_update.println("Items:" + items.size() + ", Img:" + images +", SalFace:" + salface);
                }
            }
            catch (Exception e) {
                new_debug_update.println(e);
            }

            //
        }

        // without category constraint
        PathRecommender.WeightedItemList items = mapDomainItems.get(domainId);
        if (items == null) {
            items = new PathRecommender.WeightedItemList();
            mapDomainItems.put(domainId, items);
        }
        items.add(newItem, confidence);
        //new_nocategory_up.println("***************" + domainId + ", " + System.identityHashCode(mapDomainItems.get(domainId)) + "*************");
        int counter = 0;
        int salface = 0;
        int images = 0;
        for (PathRecommender.WeightedItem item : items){
            counter ++;
            if (item.getFaces() != 0 || item.getSal() != 0){
                salface ++;
                //new_nocategory_up.println("ID:" + item.getItemId() + ", Freq:"+ item.getFreq() + ", Faces:" + item.getFaces() + ", Sal: " + item.getSal() );
            }
        }
        //new_nocategory_up.println("Items:" + items.size() + ", Img:" + images +", SalFace:" + salface);


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
