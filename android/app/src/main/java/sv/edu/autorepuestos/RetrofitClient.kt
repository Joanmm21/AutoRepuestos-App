package sv.edu.autorepuestos

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Dirección del backend desde el emulador Android
    //private const val BASE_URL = "http://10.0.2.2:3000/"

    //Direccion del backed (servidor node) desde la compu
    private const val BASE_URL = "http://192.168.1.83:3000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}