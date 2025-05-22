package api;

import java.util.List;

public class ModelResponse {
    public class LoginResponse {
        private String session_id;
        private String status;
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSessionId() {
            return session_id;
        }

        public void setSessionId(String session_id) {
            this.session_id = session_id;
        }

        public String getMessage() {
            return status;
        }

        public void setMessage(String message) {
            this.status = status;
        }
    }

    public class UserResponse {
        private String status;
        private Data data;
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public class Data {
            private User user;

            public User getUser() {
                return user;
            }

            public void setUser(User user) {
                this.user = user;
            }
        }

        public class User {
            private String id;
            private String name;
            private String email;
            private String date_of_birth;
            private String country;
            private String url_avatar;

            private List<String> saved_recipes;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getDateOfBirth() {
                return date_of_birth;
            }

            public void setDateOfBirth(String date_of_birth) {
                this.date_of_birth = date_of_birth;
            }

            public String getCountry() {
                return country;
            }

            public void setCountry(String country) {
                this.country = country;
            }

            public String getUrlAvatar() {
                return url_avatar;
            }
            public void setUrlAvatar(String url_avatar) {
                this.url_avatar = url_avatar;
            }

            public List<String> getSavedRecipes() {
                return saved_recipes;
            }

            public void setSavedRecipes(List<String> saved_recipes) {
                this.saved_recipes = saved_recipes;
            }

            // Helper method to check if a recipe is saved
            public boolean isRecipeSaved(String recipeId) {
                return saved_recipes != null && saved_recipes.contains(recipeId);
            }
        }
    }

    public class SignUpResponse {
        private String status;
        private String message;
        private UserResponse.User Data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public class ForgotPasswordResponse {
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public class VerifyOtpResponse {
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public class UpdateUserResponse {
        private String status;
        private String message;
        private UserResponse.User data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public class ChangePasswordResponse {
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public class RecipeResponse {
        private String status;
        private int results;
        private Data data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getResults() {
            return results;
        }

        public void setResults(int results) {
            this.results = results;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public class Data {
            private List<Recipe> recipes;

            public List<Recipe> getRecipes() {
                return recipes;
            }

            public void setRecipes(List<Recipe> recipes) {
                this.recipes = recipes;
            }
        }

        public class Recipe {
            private String id;
            private String title;
            private String author;
            private String image_url;
            private List<String> ingredients;
            private List<String> instructions;
            private String time;

            public String getTime() {
                return time;
            }
            public void setTime(String time) {
                this.time = time;
            }
            private List<RecipeDetailResponse.Rating> rating;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getAuthor() {
                return author;
            }

            public void setAuthor(String author) {
                this.author = author;
            }

            public String getImageUrl() {
                return image_url;
            }

            public void setImageUrl(String image_url) {
                this.image_url = image_url;
            }

            // Calculate and return the average rating
            public double getAverageRating() {
                if (rating == null || rating.isEmpty()) {
                    return 0.0;
                }

                double sum = 0.0;
                for (RecipeDetailResponse.Rating r : rating) {
                    sum += r.getRating();
                }

                return sum / rating.size();
            }

            // Getter for the rating list
            public List<RecipeDetailResponse.Rating> getRating() {
                return rating;
            }

            // Setter for the rating list
            public void setRating(List<RecipeDetailResponse.Rating> rating) {
                this.rating = rating;
            }

            // With these correct getters:
            public List<String> getIngredients() {
                return ingredients;
            }

            public List<String> getInstructions() {
                return instructions;
            }

            // Keep your existing setters, but rename them:
            public void setIngredients(List<String> ingredients) {
                this.ingredients = ingredients;
            }

            public void setInstructions(List<String> instructions) {
                this.instructions = instructions;
            }
        }
    }

    public static class RecipeDetailResponse {
        private String status;
        private RecipeData data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public RecipeData getData() {
            return data;
        }

        public void setData(RecipeData data) {
            this.data = data;
        }

        public static class RecipeData {
            private Recipe recipe;

            public Recipe getRecipe() {
                return recipe;
            }

            public void setRecipe(Recipe recipe) {
                this.recipe = recipe;
            }
        }

        public static class Recipe {
            private String id;
            private String title;
            private String author;
            private String image_url;
            private List<String> ingredients;
            private List<String> instructions;
            private String created_at;
            private String updated_at;
            private List<Comment> comments;
            private List<Rating> rating;
            private double AverageRating;
            private String time;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getAuthor() {
                return author;
            }

            public void setAuthor(String author) {
                this.author = author;
            }

            public String getImageUrl() {
                return image_url;
            }

            public void setImageUrl(String image_url) {
                this.image_url = image_url;
            }

            public List<String> getIngredients() {
                return ingredients;
            }

            public void setIngredients(List<String> ingredients) {
                this.ingredients = ingredients;
            }

            public List<String> getInstructions() {
                return instructions;
            }

            public void setInstructions(List<String> instructions) {
                this.instructions = instructions;
            }

            public String getCreatedAt() {
                return created_at;
            }

            public void setCreatedAt(String created_at) {
                this.created_at = created_at;
            }

            public String getUpdatedAt() {
                return updated_at;
            }

            public void setUpdatedAt(String updated_at) {
                this.updated_at = updated_at;
            }

            public List<Comment> getComments() {
                return comments;
            }

            public void setComments(List<Comment> comments) {
                this.comments = comments;
            }

            public List<Rating> getRatings() {
                return rating;
            }

            public void setRatings(List<Rating> rating) {
                this.rating = rating;
            }

            public String getTime() {
                return time;
            }
            public void setTime(String time) {
                this.time = time;
            }

            // Method to calculate average rating
            public double getAverageRating() {
                if (rating == null || rating.isEmpty()) {
                    return 0.0;
                }

                double sum = 0.0;
                for (Rating r : rating) {
                    sum += r.getRating();
                }
                return sum / rating.size();
            }

            // Method to get number of reviews
            public int getReviewCount() {
                return (rating != null) ? rating.size() : 0;
            }
        }

        public static class Comment {
            private String id;
            private String author_id;
            private String content;
            private String created_at;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getAuthor_id() {
                return author_id;
            }

            public void setAuthor_id(String author_id) {
                this.author_id = author_id;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public String getCreatedAt() {
                return created_at;
            }

            public void setCreatedAt(String created_at) {
                this.created_at = created_at;
            }
        }

        public static class Rating {
            private String id;
            private int rating;
            private String user_id;
            private String created_at;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public int getRating() {
                return rating;
            }

            public void setRating(int rating) {
                this.rating = rating;
            }

            public String getUserId() {
                return user_id;
            }

            public void setUserId(String user_id) {
                this.user_id = user_id;
            }

            public String getCreatedAt() {
                return created_at;
            }

            public void setCreatedAt(String created_at) {
                this.created_at = created_at;
            }
        }
    }

    public class CommentResponse {
        private String status;
        private int results;
        private Data data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getResults() {
            return results;
        }

        public void setResults(int results) {
            this.results = results;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public class Data {
            private List<Comment> comments;

            public List<Comment> getComments() {
                return comments;
            }

            public void setComments(List<Comment> comments) {
                this.comments = comments;
            }
        }

        public class Comment {
            private String id;
            private String author_id;
            private String content;
            private String created_at;

            // New fields for likes and dislikes
            private int likes;
            private int dislikes;

            // New fields for tracking who liked/disliked
            private List<String> likedBy;

            private List<String> dislikedBy;

            // Transient fields to track current user's reaction status
            private transient boolean userLiked;
            private transient boolean userDisliked;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getAuthor_id() {
                return author_id;
            }

            public void setAuthor_id(String author_id) {
                this.author_id = author_id;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public String getCreatedAt() {
                return created_at;
            }

            public void setCreatedAt(String created_at) {
                this.created_at = created_at;
            }

            public int getLikes() {
                return likes;
            }

            public void setLikes(int likes) {
                this.likes = likes;
            }

            public int getDislikes() {
                return dislikes;
            }

            public void setDislikes(int dislikes) {
                this.dislikes = dislikes;
            }

            public List<String> getLikedBy() {
                return likedBy;
            }

            public void setLikedBy(List<String> likedBy) {
                this.likedBy = likedBy;
            }

            public List<String> getDislikedBy() {
                return dislikedBy;
            }

            public void setDislikedBy(List<String> dislikedBy) {
                this.dislikedBy = dislikedBy;
            }

            public boolean isUserLiked() {
                return userLiked;
            }

            public void setUserLiked(boolean userLiked) {
                this.userLiked = userLiked;
            }

            public boolean isUserDisliked() {
                return userDisliked;
            }

            public void setUserDisliked(boolean userDisliked) {
                this.userDisliked = userDisliked;
            }

            // Helper method to get formatted date if needed
            public String getFormattedDate() {
                if (created_at != null && created_at.length() > 16) {
                    return created_at.replace("T", " ").substring(0, 16);
                }
                return created_at;
            }

            // Helper method to check if current user has liked this comment
            public void checkUserReactions(String userId) {
                if (likedBy != null) {
                    userLiked = likedBy.contains(userId);
                }
                if (dislikedBy != null) {
                    userDisliked = dislikedBy.contains(userId);
                }
            }
        }
    }

    public class SavedRecipeResponse {
        private String status;
        private String message;
        private Data data;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public class Data {
            private String savedRecipe;

            private int totalSavedRecipes;

            public String getSavedRecipe() {
                return savedRecipe;
            }

            public void setSavedRecipe(String savedRecipe) {
                this.savedRecipe = savedRecipe;
            }

            public int getTotalSavedRecipes() {
                return totalSavedRecipes;
            }

            public void setTotalSavedRecipes(int totalSavedRecipes) {
                this.totalSavedRecipes = totalSavedRecipes;
            }
        }
    }

    public class DeleteSavedRecipeResponse {
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
