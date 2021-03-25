package com.kepollo.mx.lectorimagen;

import java.io.File;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    @FormUrlEncoded
    @POST("uploadPhoto.php")
    Observable<String> uploadPhoto(@Field("token") String user,
                                   @Field("fecha") String pass,
                                   @Field("hora") String fecha,
                                   @Field("tipo") String hora/*,
                                   @("imagen") File imagen*/);

    /**
     * Probando Enviar imagen en base64
     *
     */

    @POST("uploadPhoto.php")
    Observable<String> uploadPhotoBase64(@Field("token") String user,
                                   @Field("fecha") String pass,
                                   @Field("hora") String fecha,
                                   @Field("tipo") String hora,
                                   @Field("imagen") String imagen);

}
