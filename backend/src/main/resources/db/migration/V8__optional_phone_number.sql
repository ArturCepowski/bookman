-- Q-24 follow-up (owner decision): the phone number was needed only for SMS reminders; it is now optional.
-- The E.164 CHECK from V1 still applies when a number is given (CHECK passes for NULL).
ALTER TABLE app_user ALTER COLUMN phone_number DROP NOT NULL;
