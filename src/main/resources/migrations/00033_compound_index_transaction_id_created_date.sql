--liquibase formatted sql

--changeset uk.gov.pay:compound_index_transaction_id_created_date runInTransaction:false

CREATE INDEX CONCURRENTLY IF NOT EXISTS id_created_date_idx ON transaction USING btree(id, created_date);