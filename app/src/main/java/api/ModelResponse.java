package api;

public class ModelResponse {
    public class LoginResponse{
        private String session_id;
        private String message;

        public String getSessionId() {
            return session_id;
        }

        public void setSessionId(String session_id) {
            this.session_id = session_id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
