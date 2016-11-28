package net.recommenders.plista.rec;
import java.io.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;


public class ImageEvaluation{
   private int number_faces; 
   private int binary_saliency;
   private String image_url;
   private int confidence;
   private Long id_Item;
   private PathRecommender.WeightedItem item;
   private static int counter = 0;
   private static PrintWriter out;
   static {
     PrintWriter tmp = null;
     try {
       tmp = new PrintWriter(new BufferedWriter(new FileWriter("img.log", true)));
     } catch (IOException e) {
       System.out.println(e);
     }
     out = tmp;
   }
    

   ImageEvaluation(PathRecommender.WeightedItem item){
        image_url = item.getImg_Url();
        id_Item = item.getItemId();
        this.item = item;
        counter++;
    }

   public void compute() {
	   String call = "python face_detection.py " + image_url;
		 try{
  			Process p = Runtime.getRuntime().exec(call);
  			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader in_err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String err = null;
        try {
            

            String s = null;
            boolean isfirst = true;
            while ((s = in.readLine()) != null) {
                int num = Integer.parseInt(s);
                if (isfirst){
                  number_faces = num;
                  isfirst = false;
                }
                else {
                  binary_saliency = num;
                }
            }
            while ((s = in_err.readLine()) != null) {
                out.println("ERROR! " + s);
            }

            String timeStamp = new SimpleDateFormat("yyyy_MM_dd HH:mm:ss").format(Calendar.getInstance().getTime());
            out.println(timeStamp);
            out.println("Id_Item:" + id_Item + " IMG: " + image_url);
            out.println("Faces: " +  number_faces);
            out.println("Saliency: " +  binary_saliency);
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
        out.println("Number of Threads: " + nbRunning);
  			
		}  catch (IOException e) {
			System.out.println(e);
		}
    counter--;
	}

  public int countInstances(){
        return counter;
    }

  public int tearDown(){
    counter--;
    return 0; //if cannot be processed i return a 0
  }

  public Integer getFaces(){
    return number_faces;
  }

  public Integer getSaliency(){
    return binary_saliency;
  }

      
}
