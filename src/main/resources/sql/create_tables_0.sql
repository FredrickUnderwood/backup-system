DROP TABLE if exists case_record;
DROP TABLE if exists execution_record;
DROP TABLE if exists failure_file_record;

CREATE TABLE case_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    source_path VARCHAR(1024) NOT NULL,
    backup_path VARCHAR(1024) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL
);

CREATE TABLE execution_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    case_id BIGINT NOT NULL,
    execution_type VARCHAR(64) NOT NULL,
    backup_mode VARCHAR(64) NOT NULL,
    source_path VARCHAR(1024) NOT NULL,
    destination_path VARCHAR(1024) NOT NULL,
    execution_time TIMESTAMP NOT NULL,
    is_transmit_success BOOLEAN NOT NULL,
    is_solve_diff_success BOOLEAN NOT NULL,
    is_metadata_support BOOLEAN NOT NULL,
    is_metadata_support_success BOOLEAN,
    FOREIGN KEY (case_id) REFERENCES case_record(id) ON DELETE CASCADE
);

CREATE TABLE failure_file_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
    execution_id BIGINT NOT NULL,
    failure_type VARCHAR(64) NOT NULL,
    file VARCHAR(1024) NOT NULL,
    file_type VARCHAR(1024) NOT NULL,
    FOREIGN KEY (execution_id) REFERENCES execution_record(id) ON DELETE CASCADE
);



