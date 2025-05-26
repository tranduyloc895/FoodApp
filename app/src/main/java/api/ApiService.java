package api;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // Authentication & Account Management
    @FormUrlEncoded
    @POST("auth/login")
    Call<ModelResponse.LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("auth/signup")
    Call<ModelResponse.SignUpResponse> signUp(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password,
            @Field("confirmPassword") String confirmPassword
    );

    @POST("auth/logout")
    Call<Void> logout(
            @retrofit2.http.Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("auth/forgot-password")
    Call<Void> forgotPassword(
            @Field("email") String email
    );

    @FormUrlEncoded
    @POST("auth/verify-otp")
    Call<ModelResponse.VerifyOtpResponse> verifyOtp(
            @Field("email") String email,
            @Field("otp") String otp
    );

    @FormUrlEncoded
    @PATCH("auth/reset-password")
    Call<ModelResponse.LoginResponse> resetPassword(
            @Field("email") String email,
            @Field("password") String password,
            @Field("confirmPassword") String confirmPassword
    );

    @FormUrlEncoded
    @PATCH("users/update-password")
    Call<ModelResponse.ChangePasswordResponse> updatePassword(
            @retrofit2.http.Header("Authorization") String token,
            @Field("currentPassword") String currentPassword,
            @Field("newPassword") String newPassword,
            @Field("confirmNewPassword") String confirmNewPassword
    );

    @DELETE("users/delete-account")
    Call<ModelResponse.readNotificationResponse> deleteAccount(
            @retrofit2.http.Header("Authorization") String token
    );

    // User Profile Management
    @GET("users/me")
    Call<ModelResponse.UserResponse> getUserInfo(
            @retrofit2.http.Header("Authorization") String token
    );

    @GET("users/user/{userId}")
    Call<ModelResponse.UserResponse> getUserById(
            @retrofit2.http.Header("Authorization") String token,
            @Path("userId") String userId
    );

    @FormUrlEncoded
    @PATCH("users/update-profile")
    Call<ModelResponse.UpdateUserResponse> updateProfile(
            @retrofit2.http.Header("Authorization") String token,
            @Field("name") String name,
            @Field("email") String email,
            @Field("date_of_birth") String dateOfBirth,
            @Field("country") String country
    );

    @Multipart
    @POST("users/upload-avatar")
    Call<ModelResponse.UserResponse> uploadAvatar(
            @retrofit2.http.Header("Authorization") String token,
            @Part MultipartBody.Part avatar
    );

    // Recipe Management
    @GET("recipes/get-recipe-latest")
    Call<ModelResponse.RecipeResponse> getRecipeLatest(
            @Header("Authorization") String token
    );

    @GET("recipes/get-random-recipe")
    Call<ModelResponse.RecipeResponse> getRandomRecipe(
            @retrofit2.http.Header("Authorization") String token
    );

    @GET("recipes/get-recipe-id")
    Call<ModelResponse.RecipeDetailResponse> getRecipeDetail(
            @retrofit2.http.Header("Authorization") String token,
            @Query("id") String recipeId
    );

    @GET("recipes/get-all-recipes")
    Call<ModelResponse.RecipeResponse> getAllRecipes(
            @retrofit2.http.Header("Authorization") String token
    );

    @Multipart
    @POST("recipes/add-recipe")
    Call<ModelResponse.RecipeDetailResponse> addRecipeWithParts(
            @Header("Authorization") String token,
            @PartMap Map<String, RequestBody> parts,
            @Part MultipartBody.Part imageRecipe
    );

    @DELETE("recipes/{recipeId}")
    Call<ModelResponse.readNotificationResponse> deleteRecipe(
            @retrofit2.http.Header("Authorization") String token,
            @Path("recipeId") String recipeId
    );

    @FormUrlEncoded
    @PATCH("recipes/rating-recipe")
    Call<ModelResponse.RatingResponse> rateRecipe(
            @retrofit2.http.Header("Authorization") String token,
            @Field("id") String recipeId,
            @Field("rating") int rating
    );

    @GET("recipes/get-recipe-rating")
    Call<ModelResponse.getRatingResponse> getRecipeRating(
            @retrofit2.http.Header("Authorization") String token,
            @Query("id") String recipeId
    );

    // Recipe Saved/Bookmarks
    @GET("users/saved-recipes")
    Call<ModelResponse.RecipeResponse> getSavedRecipes(
            @retrofit2.http.Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("users/save-recipe")
    Call<ModelResponse.SavedRecipeResponse> saveRecipe(
            @retrofit2.http.Header("Authorization") String token,
            @Field("recipeId") String recipeId
    );

    @DELETE("users/saved-recipes/{recipeId}")
    Call<ModelResponse.DeleteSavedRecipeResponse> deleteSavedRecipe(
            @retrofit2.http.Header("Authorization") String token,
            @Path("recipeId") String recipeId
    );

    // Comments
    @GET("recipes/get-recipe-comments")
    Call<ModelResponse.CommentResponse> getRecipeComments(
            @retrofit2.http.Header("Authorization") String token,
            @Query("id") String recipeId
    );

    @FormUrlEncoded
    @PATCH("recipes/comment-recipe")
    Call<ModelResponse.RecipeDetailResponse> postComment(
            @retrofit2.http.Header("Authorization") String token,
            @Field("id") String recipeId,
            @Field("comment") String comment
    );

    @FormUrlEncoded
    @PATCH("recipes/like-comment")
    Call<ModelResponse.RecipeDetailResponse> likeComment(
            @retrofit2.http.Header("Authorization") String token,
            @Field("recipeId") String recipeId,
            @Field("commentId") String commentId
    );

    @FormUrlEncoded
    @PATCH("recipes/dislike-comment")
    Call<ModelResponse.RecipeDetailResponse> dislikeComment(
            @retrofit2.http.Header("Authorization") String token,
            @Field("recipeId") String recipeId,
            @Field("commentId") String commentId
    );

    @DELETE("comments/{commentId}")
    Call<ModelResponse.readNotificationResponse> deleteComment(
            @retrofit2.http.Header("Authorization") String token,
            @Path("commentId") String commentId
    );

    // Notifications
    @GET("notifications/")
    Call<ModelResponse.NotificationsResponse> getNotifications(
            @retrofit2.http.Header("Authorization") String token
    );

    @PATCH("notifications/{notificationId}/read")
    Call<ModelResponse.readNotificationResponse> markNotificationAsRead(
            @retrofit2.http.Header("Authorization") String token,
            @Path("notificationId") String notificationId
    );

    @DELETE("notifications/{notificationId}")
    Call<ModelResponse.readNotificationResponse> deleteNotification(
            @retrofit2.http.Header("Authorization") String token,
            @Path("notificationId") String notificationId
    );

    // AI search
    @Multipart
    @POST("ai/search-image")
    Call<ModelResponse.searchImageResponse> searchImage(
            @retrofit2.http.Header("Authorization") String token,
            @Part MultipartBody.Part image
    );
}