import java.net.URL;
import java.awt.image.BufferedImage;
import java.io.*;
import java.awt.Image;

public class ImageEvaluation 
{ 
	public static void main(String [ ] args)
	{
		ImageEvaluationThread face = new ImageEvaluationThread("THE URL OF THE IMAGE");
		face.run();
		try {
		face.join();
		}catch (InterruptedException e){
			System.out.println(e);
		}
		

	}



	private static class ImageEvaluationThread extends Thread {
	   private String image_url;
	   private int number_faces; 
	   
	   ImageEvaluationThread(String _url){
		   image_url = _url;
		}
	   public void run() {
		    String call = "python face_detection.py " + image_url;
                    String ret = null;
                    int confidence = 0; 
                    try{
                            Process p = Runtime.getRuntime().exec(call);
                            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            BufferedReader in_err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("img.log", true)));
			    String err = null;
                            try {
                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                                out.println(timeStamp);
                                out.println("IMG: " + image_url);
                                String s = null;
                                while((s = in.readLine())==null && ((err = in_err.readLine())==null)){
                                    //TODO terminate after a while and return nothing
                                }
                                ret = s;
                                number_faces = Integer.parseInt(ret);
                                out.println("Faces: " +  number_faces);
                            }catch (IOException e) {
                                out.println("ERROR! " + e);
				out.println("HERE: " + err);
                               //TODO continue from here. Join the ImageEvaluation with this Class. make the while to wait for the exact output. (i just changed face_dec.py so it does not output url
                               //here catch the exception and print it in img.log
                               //remember to put the haar xml file when compiling
                            }
                            
                            //these value needs to be tuned
                            if (number_faces == 1){
                                confidence = confidence + 9; // so starts with 10
                            }
                            else if (number_faces > 1){
                                confidence = confidence + 4; // so starts with 5
                            }
                    }  catch (IOException e) {
                            System.out.println(e);
                    }
 
                    callIt(confidence);
		}

	private void callIt(int hi){
		System.out.println(hi);
	}

		  
	}
}
