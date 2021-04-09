package com.kepollo.mx.lectorimagen;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "INFORMACION";
    private ImageView mImageView;
    private Button mTextButton, camera_pick_galery, desplegar_detalle;
    private Bitmap mSelectedImage;
    // Max width (portrait mode)
    private Integer mImageMaxWidth;
    // Max height (portrait mode)
    private Integer mImageMaxHeight;

    public static final int REQUEST_CODE_TAKE_PHOTO = 0 /*1*/;
    private String mCurrentPhotoPath;
    private Uri photoURI;
    private Ticket ticket;
    private Vale vale;


    private static final int PICK_IMAGE = 100;

    /**
     * Number of results to show in the UI.
     */
    private static final int RESULTS_TO_SHOW = 3;
    /**
     * Dimensions of inputs.
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int DIM_IMG_SIZE_X = 224;
    private static final int DIM_IMG_SIZE_Y = 224;

    private final PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float>
                                o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });
    /* Preallocated buffers for storing image data. */
    private final int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imageView);

        mTextButton = findViewById(R.id.camera_capture_button);
        camera_pick_galery = findViewById(R.id.camera_pick_galery);
        desplegar_detalle = findViewById(R.id.desplegar_detalle);


        mTextButton.setOnClickListener(v -> {
            checkExternalStoragePermission();
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {


                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            225);
                }


                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.CAMERA)) {

                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            226);
                }
            } else {
                dispatchTakePictureIntent();
            }
        });

        camera_pick_galery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        desplegar_detalle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Desplegar Informacion
            }
        });

        ticket = new Ticket();
        vale = new Vale();
    }

    private void runTextRecognition() {
        //mSelectedImage = getBitmapFromAsset(this, photoURI.toString());
        Log.e(TAG, "SE EMPEZARA A PROCESAR LA INFOMACION");
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        //mTextButton.setEnabled(false);
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text texts) {
                                //mTextButton.setEnabled(true);
                                Log.e(TAG, "TODO VA BIEN");
                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                Log.e(TAG, "ALGO SALIO MAL");
                                e.printStackTrace();
                            }
                        });
    }

    private void processTextRecognitionResult(Text texts) {
        Log.e(TAG, "processTextRecognitionResult");
        List<Text.TextBlock> blocks = texts.getTextBlocks();

        Log.e(TAG, "BLOCK SIZE: " + blocks.size());
        if (blocks.size() == 0) {
            Log.e(TAG, "No text found");
            showToast("No text found");
            return;
        }

        convertTextResultAtTicket(blocks);

    }


    private void convertTextResultAtTicket(List<Text.TextBlock> blocks) {
        List<String> searchrfc = new ArrayList<>();
        searchrfc.add("RFC");
        searchrfc.add("R.F.C");

        List<String> searchTransaccion = new ArrayList<>();
        searchTransaccion.add("TRANSACCION NO.");
        searchTransaccion.add("TRANSACCION");
        searchTransaccion.add("Transaccion");
        searchTransaccion.add("Referencia");

        int numBlockVale = -1;
        for (int i = 0; i < blocks.size(); i++) {
            Log.e(TAG, "BLOQUE: " + i);
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                //Log.e(TAG, "LINEA: "+lines.get(j).getElements());
                List<Text.Element> elements = lines.get(j).getElements();
                String infoconsult = getAllLine(elements).toUpperCase();
                if(infoconsult.toUpperCase().contains("VALE DE COMBUSTIBLE A:") || infoconsult.toUpperCase().contains("VALE DE COMBUSTIBLE")) {
                    numBlockVale = i;
                }

                if( numBlockVale == (i + 1)) {
                    String asignado = getAllLine(lines.get(j+1).getElements()).toUpperCase();
                    if(asignado.contains("DEPARTAMENTO")) {
                        //No tiene para quien es el vale

                    } else {
                      //Si tiene para quien es
                      vale.setAsiganado(asignado);
                    }
                }
                /**
                 *
                 * Esta opcion es especial para los vales
                 *
                 */

                for (int k = 0; k < elements.size(); k++) {
                    Log.e(TAG, "DATA: " + elements.get(k).getText());

                    if (elements.get(k).getText().toUpperCase().contains("FOLIO")) {
                        vale.setFolio(elements.get(k+1).getText());
                    }

                }

                /**
                 *
                 * Esta seccion es para los tickets
                 *
                 */
                //Log.e(TAG, "elemento: "+infoconsult);
                                   /* if (infoconsult.contains("CUATRO CAMINOS") || infoconsult.contains("CUATROCAMINOS") || infoconsult.contains("CAMINOS")) {
                                        ticket.setRfc("4caminos");
                                        ticketCuatroCaminos(blocks);
                                    }
                                    if ((i < 2) && j == 0) {
                                        //Se obtiene el proveedor
                                        if (infoconsult.contains("S.A") || infoconsult.contains("C.V")) {
                                            ticket.setProvedor(infoconsult);
                                        }
                                        if (infoconsult.contains("KOPLA") || infoconsult.contains("kopla")) {
                                            ticket.setRfc("RFCKOPLA");
                                            ticketKopla(blocks);
                                            break;
                                        }

                                        if (infoconsult.contains("SUPER SERVICIOS DE COMBUSTIBLE HUACUZ") || infoconsult.contains("SUPER SERVICIOS")
                                            || infoconsult.contains("COMBUSTIBLE HUACUZ") || infoconsult.contains("HUACUZ") || infoconsult.contains("GASOLINERA DE TACAMBARO")
                                                ||  infoconsult.contains("TACAMBARO")) {
                                            ticket.setRfc("RFCSUPERSERVICIOS");
                                            ticketSuperServicios(blocks);
                                            break;
                                        }
                                    }
                                    if (infoconsult.contains("RFC") || infoconsult.contains("R.F.C")) {
                                        if (ticket.getRfc().equals("0")) {
                                            ticket.setRfc(getElementAfterElement(elements, searchrfc));
                                        }
                                    }
                                    if (infoconsult.contains("TRANSACCION NO.") || infoconsult.contains("TRANSACCION") || infoconsult.contains("Transaccion") || infoconsult.contains("Referencia")) {
                                        ticket.setRfc(getElementAfterElement(elements, searchTransaccion));
                                    }*/

                //for (int k = 0; k < elements.size(); k++) {
                    //Log.e(TAG, "DATA: " + elements.get(k).getText());

                   /* if (matches_date(elements.get(k).getText())) {
                        ticket.setFecha(elements.get(k).getText());
                    }

                    if (matches_hour(elements.get(k).getText())) {
                        ticket.setHora(elements.get(k).getText());
                    }

                    if(elements.get(k).getText().contains("MAGNA") || elements.get(k).getText().contains("PREMIUM") || elements.get(k).getText().contains("DIESEL")) {
                        ticket.setCombustible(elements.get(k).getText());
                        ticket.setLitros(elements.get(k-1).getText());
                        ticket.setTypeCombustible(check_type_comb(elements.get(k).getText()));
                        numBlockImporte = i +1;
                    }

                    if(numBlockImporte == i) {
                        Log.e(TAG, "DATAaaaa: " + elements.get(k).getText());
                        ticket.setPunit(elements.get(k).getText());
                        ticket.setImporte(elements.get(k+1).getText());
                        numBlockImporte = 0;
                    }*/
                //}
            }
        }

        Log.e(TAG, vale.toString());
    }

    private void ticketSuperServicios(List<Text.TextBlock> blocks) {
        Log.e(TAG, "ES UN TICKET DE SUPERFICIE");
        int numBlockImporte = -1;
        for (int i = 0; i < blocks.size(); i++) {
            Log.e(TAG, "BLOQUE: " + i);
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                String infoconsult = getAllLine(elements);
                //Log.e(TAG, "elemento: "+infoconsult);
                if ((i < 2) && j == 0) {
                    //Se obtiene el proveedor
                    if (infoconsult.contains("S.A") || infoconsult.contains("C.V")) {
                        ticket.setProvedor(infoconsult);
                    }
                    if (infoconsult.contains("R.F.C.") || infoconsult.contains("R.F.C")) {
                        //ticket.setRfc("RFCKOPLA");
                    }
                }

                for (int k = 0; k < elements.size(); k++) {
                    Log.e(TAG, "DATA: " + elements.get(k).getText());
                    if (matches_date(elements.get(k).getText())) {
                        ticket.setFecha(elements.get(k).getText());
                    }

                    if (matches_hour(elements.get(k).getText())) {
                        ticket.setHora(elements.get(k).getText());
                    }
                    if (elements.get(k).getText().contains("Transaccion") || elements.get(k).getText().contains("Transaccion:") || elements.get(k).getText().contains("Trainsaccion:") || elements.get(k).getText().contains("Trainsaccion:") ) {
                        ticket.setNoTransaccion(elements.get(k+1).getText());
                    }
                    if (elements.get(k).getText().contains("R.F.C.") || elements.get(k).getText().contains("R.F.C") ) {
                        //ticket.setRfc(elements.get(k+1).getText());
                    }

                    if(elements.get(k).getText().toUpperCase().contains("REGULAR") || elements.get(k).getText().toUpperCase().contains("MAGNA") || elements.get(k).getText().toUpperCase().contains("DIESEL") ) {
                        ticket.setCombustible(elements.get(k).getText());
                        numBlockImporte = i +1;
                    }

                    if(numBlockImporte == i) {
                        Log.e(TAG, "DATAaaaa: " + elements.get(k).getText());
                        ticket.setLitros(elements.get(k).getText().replace("Lt", ""));
                        ticket.setPunit(elements.get(k+1).getText().replace("$", "").replace(",", ""));
                        if(elements.size() > (k + 3)) {
                            ticket.setImporte(elements.get(k+3).getText().replace("$", ""));
                        }else {
                            ticket.setImporte(elements.get(k+2).getText().replace("$", ""));
                        }
                        numBlockImporte = 0;
                    }
                }
            }
        }
        Log.e(TAG, "tickert: " + ticket.toString());
    }

    private void ticketCuatroCaminos(List<Text.TextBlock> blocks) {
        Log.e(TAG, "ES UN TICKET DE 4 CAMINOS");
        for (int i = 0; i < blocks.size(); i++) {
            Log.e(TAG, "BLOQUE: " + i);
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                String infoconsult = getAllLine(elements);
                //Log.e(TAG, "elemento: "+infoconsult);
                if ((i < 2) && j == 0) {
                    //Se obtiene el proveedor
                    if (infoconsult.contains("S.A") || infoconsult.contains("C.V")) {
                        ticket.setProvedor(infoconsult);
                    }
                    if (infoconsult.contains("R.F.C.") || infoconsult.contains("R.F.C")) {
                        //ticket.setRfc("RFCKOPLA");
                    }
                }

                for (int k = 0; k < elements.size(); k++) {
                    Log.e(TAG, "DATA: " + elements.get(k).getText());
                    if (matches_date(elements.get(k).getText())) {
                        ticket.setFecha(elements.get(k).getText());
                    }

                    if (matches_hour(elements.get(k).getText())) {
                        ticket.setHora(elements.get(k).getText());
                    }

                    if (elements.get(k).getText().contains("R.F.C.") || elements.get(k).getText().contains("R.F.C") ) {
                        ticket.setRfc(elements.get(k+1).getText());
                    }

                    if(elements.get(k).getText().toUpperCase().contains("MAGNA") || elements.get(k).getText().toUpperCase().contains("PREMIUM") || elements.get(k).getText().toUpperCase().contains("DIESEL")) {
                        ticket.setCombustible(elements.get(k).getText());
                        ticket.setLitros(elements.get(k+1).getText());
                        ticket.setPunit(elements.get(k+2).getText());
                        ticket.setImporte(elements.get(k+3).getText());
                        ticket.setTypeCombustible(check_type_comb(elements.get(k).getText()));
                        //numBlockImporte = i +1;
                    }

                    /*if(numBlockImporte == i) {
                        Log.e(TAG, "DATAaaaa: " + elements.get(k).getText());
                        ticket.setPunit(elements.get(k).getText());
                        ticket.setImporte(elements.get(k+1).getText());
                        numBlockImporte = 0;
                    }*/
                }
            }
        }

        Log.e(TAG, "tickert: " + ticket.toString());
    }

    public void ticketKopla(List<Text.TextBlock> blocks) {
        Log.e("INFORMACION", "ES UN TICKET DE KOPLA");
        int numBlockImporte = -1;
        for (int i = 0; i < blocks.size(); i++) {
            Log.e(TAG, "BLOQUE: " + i);
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                //Log.e(TAG, "LINEA: "+lines.get(j).getElements());
                List<Text.Element> elements = lines.get(j).getElements();
                String infoconsult = getAllLine(elements);
                if ((i < 2) && j == 0) {
                    //Se obtiene el proveedor
                    if (infoconsult.contains("S.A") || infoconsult.contains("C.V")) {
                        ticket.setProvedor(infoconsult);
                    }
                    if (infoconsult.contains("KOPLA") || infoconsult.contains("kopla")) {
                        ticket.setRfc("RFCKOPLA");
                    }
                }
                for (int k = 0; k < elements.size(); k++) {
                    Log.e(TAG, "DATA: " + elements.get(k).getText());
                    if (matches_date(elements.get(k).getText())) {
                        ticket.setFecha(elements.get(k).getText());
                    }

                    if (matches_hour(elements.get(k).getText())) {
                        ticket.setHora(elements.get(k).getText());
                    }

                    if(elements.get(k).getText().toUpperCase().contains("MAGNA") || elements.get(k).getText().toUpperCase().contains("PREMIUM") || elements.get(k).getText().toUpperCase().contains("DIESEL")) {
                        ticket.setCombustible(elements.get(k).getText());
                        ticket.setLitros(elements.get(k-1).getText());
                        ticket.setTypeCombustible(check_type_comb(elements.get(k).getText()));
                        numBlockImporte = i +1;
                    }

                    if(numBlockImporte == i) {
                        Log.e(TAG, "DATAaaaa: " + elements.get(k).getText());
                        ticket.setPunit(elements.get(k).getText());
                        ticket.setImporte(elements.get(k+1).getText());
                        numBlockImporte = 0;
                    }
                }
            }
        }
        Log.e(TAG, "tickert: " + ticket.toString());
    }

    private static Pattern DATE_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}$");

    private static Pattern DATE_PATTERN_2 = Pattern.compile(
            "^\\d{4}/\\d{2}/\\d{2}$");

    private static Pattern DATE_PATTERN_3 = Pattern.compile(
            "^\\d{2}/\\d{2}/\\d{4}$");

    private static Pattern HOUR_PATTERN = Pattern.compile(
            "^\\d{2}:\\d{2}:\\d{2}$");
    private static Pattern HOUR_PATTERN2 = Pattern.compile(
            "^\\d{2}:\\d{2}$");

    public boolean matches_date(String date) {
        return DATE_PATTERN.matcher(date).matches() || DATE_PATTERN_2.matcher(date).matches() || DATE_PATTERN_3.matcher(date).matches();
    }

    public boolean matches_hour(String date) {
        return HOUR_PATTERN.matcher(date).matches() || HOUR_PATTERN2.matcher(date).matches();
    }

    public int check_type_comb(String combustible) {
        int result = 0;
        switch (combustible.toUpperCase()) {
            case "MAGNA":
                result = 1;
                break;
            case "PREMIUN":
                result = 2;
                break;
            case "DIESEL":
                result = 3;
                break;
            default:
                result = 0;
        }
        return result;
    }

    private String getAllLine(List<Text.Element> elements) {
        StringBuilder result = new StringBuilder();
        for (int k = 0; k < elements.size(); k++) {
            result.append(elements.get(k).getText());
        }
        return String.valueOf(result);
    }

    public String getElementAfterElement(List<Text.Element> elements, List<String> search) {
        for (int k = 0; k < elements.size(); k++) {
            for (int i = 0; i < search.size(); i++) {
                if (elements.get(k).getText().contains(search.get(i))) {
                    if ((k + 1) < elements.size()) {
                        return elements.get(k + 1).getText();
                    }
                }
            }
        }
        return "0";
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Functions for loading images from app assets.

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxWidth() {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = mImageView.getWidth();
        }

        return mImageMaxWidth;
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxHeight() {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight =
                    mImageView.getHeight();
        }

        return mImageMaxHeight;
    }

    private void checkExternalStoragePermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Permission not granted WRITE_EXTERNAL_STORAGE.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        225);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted CAMERA.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        226);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap;
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            photoURI = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                mImageView.setImageBitmap(bitmap);
                mSelectedImage = bitmap;
                uploadPhotoAtServer(bitmap);
                runTextRecognition();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                getImageRotation(photoURI, MainActivity.this);
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                mImageView.setImageBitmap(bitmap);
                mSelectedImage = bitmap;

                /*if (mSelectedImage != null) {
                    // Get the dimensions of the View
                    Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                    int targetWidth = targetedSize.first;
                    int maxHeight = targetedSize.second;

                    // Determine how much to scale down the image
                    float scaleFactor =
                            Math.max(
                                    (float) mSelectedImage.getWidth() / (float) targetWidth,
                                    (float) mSelectedImage.getHeight() / (float) maxHeight);

                    Bitmap resizedBitmap =
                            Bitmap.createScaledBitmap(
                                    mSelectedImage,
                                    (int) (mSelectedImage.getWidth() / scaleFactor),
                                    (int) (mSelectedImage.getHeight() / scaleFactor),
                                    true);

                    //mImageView.setImageBitmap(resizedBitmap);
                    mSelectedImage = resizedBitmap;
                }
                */

                uploadPhotoAtServer(bitmap);
                runTextRecognition();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadPhotoAtServer(Bitmap bitmap) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(loggingInterceptor);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("C:/xampper/htdocs/GUARDARFOTOS/")
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(builder.build())
                .build();

        ApiService service = retrofit.create(ApiService.class);
        String picture = convertBitmapAtString(bitmap);

        Observable<String> resp = service.uploadPhotoBase64("MANUEL", "1234", "24/03/21","14:25:45", picture);
                resp.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<String>() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                disposable = d;
                            }

                            @Override
                            public void onNext(@io.reactivex.rxjava3.annotations.NonNull String usuario) {
                                Log.e(TAG,"TODO EN ORDEN SE SUBIO LA FOTO");
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.e(TAG,"HUEBO UN ERROP");
                            }

                            @Override
                            public void onComplete() {
                                disposable.dispose();
                                Log.e(TAG,"SE COMPLETO LA SUBIDA DE LA INFORMACION");
                            }
                        });
    }

    public float getImageRotation(Uri path, Context context) {
        try {

            ExifInterface exif = new ExifInterface(path.getPath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.e(TAG, "ROTACION: " + rotation);
            String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};

            Cursor cursor = context.getContentResolver().query(path, projection, null, null, null);

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            cursor.close();

        } catch (Exception ex) {
            return 0f;
        }

        return 0f;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "MyPicture");
                values.put(MediaStore.Images.Media.DESCRIPTION, "Photo taken on " + System.currentTimeMillis());
                photoURI = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                //Uri photoURI = FileProvider.getUriForFile(AddActivity.this, "com.example.android.fileprovider", photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);
            }
        }
    }

    File image;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    Disposable disposable = null;

    public String convertBitmapAtString(Bitmap image){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm = Bitmap
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);//imagen codificada
        return encodedImage;
    }

   /* public void sendPhoto(File image) {


        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(loggingInterceptor);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.114/Pruebas2/php/")
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(builder.build())
                .build();

        ApiService service = retrofit.create(ApiService.class);


        Observable<String> resp = service.uploadPhoto("MANUEL", "1234", "24/03/21","14:25:45"*//*, image*//*);
        resp.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull String usuario) {

                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });
    }*/
}

