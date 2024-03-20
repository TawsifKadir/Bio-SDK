package com.kit.fingerprintcapture.model;

import com.kit.biometricsdk.R;

public enum FingerprintID {
    RIGHT_THUMB(1,"right_thumb", R.drawable.r_thumb_blue,R.drawable.r_thumb_green,R.drawable.r_thumb_orange,R.drawable.r_thumb_red),
    RIGHT_INDEX(2,"right_index",R.drawable.r_index_blue,R.drawable.r_index_green,R.drawable.r_index_orange,R.drawable.r_index_red),
    RIGHT_MIDDLE(3,"right_middle",R.drawable.r_middle_blue,R.drawable.r_middle_green,R.drawable.r_middle_orange,R.drawable.r_middle_red),
    RIGHT_RING(4,"right_ring",R.drawable.r_ring_blue,R.drawable.r_ring_green,R.drawable.r_ring_orange,R.drawable.r_ring_red),
    RIGHT_SMALL(5,"right_small",R.drawable.r_small_blue,R.drawable.r_small_green,R.drawable.r_small_orange,R.drawable.r_small_red),
    LEFT_THUMB(6,"left_thumb",R.drawable.l_thumb_blue,R.drawable.l_thumb_green,R.drawable.l_thumb_orange,R.drawable.l_thumb_red),
    LEFT_INDEX(7,"left_index",R.drawable.l_index_blue,R.drawable.l_index_green,R.drawable.l_index_orange,R.drawable.l_index_red),
    LEFT_MIDDLE(8,"left_middle",R.drawable.l_middle_blue,R.drawable.l_middle_green,R.drawable.l_middle_orange,R.drawable.l_middle_red),
    LEFT_RING(9,"left_ring",R.drawable.l_ring_blue,R.drawable.l_ring_green,R.drawable.l_ring_orange,R.drawable.l_ring_red),
    LEFT_SMALL(10,"left_small",R.drawable.l_small_blue,R.drawable.l_small_green,R.drawable.l_small_orange,R.drawable.l_small_red);

   private int id;
   private String name;
   private int fpCapturedGoodViewID;
    private int fpCapturedBadViewID;
   private int fpCaptureFailedViewID;
   private int fpCaptureInitViewID;

   private FingerprintID(int id,String name,int fpCaptureInitViewID,int fpCapturedGoodViewID,int fpCapturedBadViewID,int fpCaptureFailedViewID){
       this.id = id;
       this.name = name;
       this.fpCaptureInitViewID = fpCaptureInitViewID;
       this.fpCapturedGoodViewID = fpCapturedGoodViewID;
       this.fpCapturedBadViewID = fpCapturedBadViewID;
       this.fpCaptureFailedViewID = fpCaptureFailedViewID;
   }

   public int getID(){
       return id;
   }
   public String getName(){return this.name;}

    public int getFpCapturedGoodViewID() {
        return fpCapturedGoodViewID;
    }

    public int getFpCapturedBadViewID() {
        return fpCapturedBadViewID;
    }

    public int getFpCaptureFailedViewID() {
        return fpCaptureFailedViewID;
    }

    public int getFpCaptureInitViewID() {
        return fpCaptureInitViewID;
    }

    public static FingerprintID getFingerprintID(int id){
       for(FingerprintID nowID:FingerprintID.values()){
           if(nowID.getID()==id){
               return nowID;
           }
       }
       return null;
   }

}
