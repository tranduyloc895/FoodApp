package api;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
public interface ApiService {
    @FormUrlEncoded
    @POST("login/")
    Call<ModelResponse.LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @GET("me/")
    Call<ModelResponse.UserResponse> getUserInfo(
            @retrofit2.http.Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("signup/")
    Call<ModelResponse.SignUpResponse> signUp(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password,
            @Field("confirm_password") String confirmPassword
    );
    @POST("logout/")
    Call<Void> logout(
            @retrofit2.http.Header("Authorization") String token
    );
    @FormUrlEncoded
    @POST("forgot-password/")
    Call<Void> forgotPassword(
            @Field("email") String email
    );
    @FormUrlEncoded
    @PATCH("verify-otp/")
    Call<ModelResponse.LoginResponse> verifyOtp(
            @Field("email") String email,
            @Field("otp") String otp,
            @Field("password") String password,
            @Field("confirmPassword") String confirmPassword
    );
}
