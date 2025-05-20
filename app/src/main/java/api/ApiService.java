package api;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @FormUrlEncoded
    @POST("auth/login/")
    Call<ModelResponse.LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @GET("auth/me/")
    Call<ModelResponse.UserResponse> getUserInfo(
            @retrofit2.http.Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("auth/signup/")
    Call<ModelResponse.SignUpResponse> signUp(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password,
            @Field("confirm_password") String confirmPassword
    );
    @POST("auth/logout/")
    Call<Void> logout(
            @retrofit2.http.Header("Authorization") String token
    );
    @FormUrlEncoded
    @POST("auth/forgot-password/")
    Call<Void> forgotPassword(
            @Field("email") String email
    );
    @FormUrlEncoded
    @POST("auth/verify-otp/")
    Call<ModelResponse.VerifyOtpResponse> verifyOtp(
            @Field("email") String email,
            @Field("otp") String otp
    );

    @FormUrlEncoded
    @PATCH("auth/reset-password/")
    Call<ModelResponse.LoginResponse> resetPassword(
            @Field("email") String email,
            @Field("password") String password,
            @Field("confirmPassword") String confirmPassword
    );

    @FormUrlEncoded
    @PATCH("auth/update-profile/")
    Call<ModelResponse.UpdateUserResponse> updateProfile(
            @retrofit2.http.Header("Authorization") String token,
            @Field("name") String name,
            @Field("email") String email,
            @Field("date_of_birth") String dateOfBirth,
            @Field("country") String country
    );

    @FormUrlEncoded
    @PATCH("auth/update-password/")
    Call<ModelResponse.ChangePasswordResponse> updatePassword(
            @retrofit2.http.Header("Authorization") String token,
            @Field("currentPassword") String currentPassword,
            @Field("newPassword") String newPassword,
            @Field("confirmNewPassword") String confirmNewPassword
    );

    @GET("recipes/get-recipe-latest/")
    Call<ModelResponse.RecipeResponse> getRecipeLatest(
            @Header("Authorization") String token
    );

    @GET("recipes/get-recipe-id/")
    Call<ModelResponse.RecipeDetailResponse> getRecipeDetail(
            @retrofit2.http.Header("Authorization") String token,
            @Query("id") String recipeId
    );

    @GET("recipes/get-recipe-comments/")
    Call<ModelResponse.CommentResponse> getRecipeComments(
            @retrofit2.http.Header("Authorization") String token,
            @Query("id") String recipeId
    );

    @FormUrlEncoded
    @PATCH("recipes/comment-recipe/")
    Call<ModelResponse.RecipeDetailResponse> postComment(
            @retrofit2.http.Header("Authorization") String token,
            @Field("id") String recipeId,
            @Field("comment") String comment
    );

    @FormUrlEncoded
    @PATCH("recipes/like-comment/")
    Call<ModelResponse.RecipeDetailResponse> likeComment(
            @retrofit2.http.Header("Authorization") String token,
            @Field("recipeId") String recipeId,
            @Field("commentId") String commentId
    );

    @FormUrlEncoded
    @PATCH("recipes/dislike-comment/")
    Call<ModelResponse.RecipeDetailResponse> dislikeComment(
            @retrofit2.http.Header("Authorization") String token,
            @Field("recipeId") String recipeId,
            @Field("commentId") String commentId
    );

    @POST("recipes/add-recipe/")
    Call<ModelResponse.RecipeResponse> addRecipe(
            @retrofit2.http.Header("Authorization") String token,
            @Body Map<String, Object> recipeData
    );
}