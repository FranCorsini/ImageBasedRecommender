package net.recommenders.plista.rec;
import java.io.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;


public class ImageEvaluationThread extends Thread {
   private int number_faces; 
   private String image_url;
   private int confidence;
   private Long id_Item;
   private PathRecommender.WeightedItem item;

   ImageEvaluationThread(PathRecommender.WeightedItem item){
        image_url = item.getImg_Url();
        id_Item = item.getItemId();
        this.item = item;
    }

   public void run() {
	   String call = "python face_detection.py " + image_url;
		 String ret = null;
		 try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("img.log", true)))){
  			Process p = Runtime.getRuntime().exec(call);
  			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader in_err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String err = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
            out.println(timeStamp);
            out.println("Id_Item:" + id_Item + "IMG: " + image_url);
            String s = null;
            while((s ==null )&& (err==null)){
                s = in.readLine();
                err = in_err.readLine();
            }
            ret = s;
            number_faces = Integer.parseInt(ret);
            out.println("Faces: " +  number_faces);
        }catch (Exception e) {
            out.println("ERROR! " + e);
            out.println("HERE! " + err);
            while((err = in_err.readLine()) != null){
                out.println(err);
                }
            number_faces = 0;
        }
        int nbRunning = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getState()==Thread.State.RUNNABLE) nbRunning++;
        }
        out.println("NUMBER_THREADS: " + nbRunning);
  			
		}  catch (IOException e) {
			System.out.println(e);
		}
    item.setFaces(number_faces);
    return;
	}

      
}
