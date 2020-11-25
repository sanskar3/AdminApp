package com.lecture1.adminapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class activityUploadPdf extends AppCompatActivity {

    private CardView addPdf;
    private EditText pdfTitle;
    private Button uploadPdfBtn;
    private TextView pdfTextView;
    private String pdfName, title;


    private final int REQ = 1;
    private Uri pdfData;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_pdf);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        pd = new ProgressDialog(this);

        addPdf = findViewById(R.id.addPdf);
        pdfTitle = findViewById(R.id.pdfTitle);
        uploadPdfBtn = findViewById(R.id.uploadPdfBtn);
        pdfTextView = findViewById(R.id.pdfTextView);

        addPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        uploadPdfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title=pdfTitle.getText().toString();

                if (title.isEmpty()){
                    pdfTitle.setError("empty");
                    pdfTitle.requestFocus();
                }else if (pdfData==null){
                    Toast.makeText(activityUploadPdf.this,"please upload pdf ",Toast.LENGTH_SHORT).show();
                }else {
                    uploadPdf();
                }
            }
        });
    }

    private void uploadPdf() {
        pd.setTitle("please  wait");
        pd.setMessage("Uploading pdf");
        pd.show();

        StorageReference reference = storageReference.child("pdf/"+pdfName+"-"+System.currentTimeMillis()+".pdf");
        reference.putFile(pdfData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri uri=uriTask.getResult();
                UploadData(String.valueOf(uri));

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(activityUploadPdf.this,"something went wrong",Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void UploadData(String downloadUrl) {
        String uniqueKey=databaseReference.child("pdf").push().getKey();

        HashMap data=new HashMap();
        data.put("pdfTitle","tittle");
        data.put("pdfUri",downloadUrl);

        databaseReference.child("pdf").child("uniqueKey").setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                pd.dismiss();
                Toast.makeText(activityUploadPdf.this,"Pdf uploaded successfully ",Toast.LENGTH_LONG).show();
                pdfTitle.setText("");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(activityUploadPdf.this,"failed to upload Pdf",Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openGallery() {
            Intent intent = new Intent();
            intent.setType("application/pdf");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Pdf File"), REQ);
    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ && resultCode == RESULT_OK){
          pdfData=data.getData();

          if( pdfData.toString().startsWith("content://")){
              Cursor cursor=null;
              try {
                  cursor=activityUploadPdf.this.getContentResolver().query(pdfData,null,null,null,null);
                  if (cursor!=null && cursor.moveToFirst()){
                      pdfName=cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                  }
              } catch (Exception e) {
                  e.printStackTrace();
              }

          }else if (pdfData.toString().startsWith("file://")){
              pdfName=new File(pdfData.toString()).getName();
          }
          pdfTextView.setText(pdfName);
        }
    }
}