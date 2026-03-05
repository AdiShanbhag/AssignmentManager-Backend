-- By Claude - Creates device_tokens table for Story 11.2
CREATE TABLE device_tokens (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                               token VARCHAR(255) NOT NULL,
                               platform VARCHAR(20) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_device_tokens_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_device_tokens_token ON device_tokens(token);
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);