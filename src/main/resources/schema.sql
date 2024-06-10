CREATE OR REPLACE TABLE `TB_FILE`
(
    `FILE_ID`       varchar(50)  NOT NULL COMMENT '파일ID',
    `FILE_PATH`     varchar(500) NOT NULL COMMENT '파일경로',
    `FILE_NM`       varchar(255) NOT NULL COMMENT '파일명',
    `FILE_XTNS`     varchar(100) NOT NULL COMMENT '파일확장자',
    `FILE_SIZE`     bigint(20)   NOT NULL COMMENT '파일크기',
    `SVC_CD`        varchar(20)  NOT NULL COMMENT '서비스코드',
    `TBL_NM`        varchar(100) COMMENT '테이블명',
    `DSTN_CLMN_VAL` varchar(500) COMMENT '식별컬럼값',
    `PROC_PRGM_ID`  varchar(50) COMMENT '처리프로그램ID',
    `RGST_PROCR_ID` varchar(50) COMMENT '등록처리자ID',
    `RGST_PROC_DTM` datetime(6) DEFAULT current_timestamp(6) COMMENT '등록처리일시',
    `UPDT_PROCR_ID` varchar(50) DEFAULT NULL COMMENT '수정처리자ID',
    `UPDT_PROC_DTM` datetime(6) DEFAULT NULL COMMENT '수정처리일시',
    PRIMARY KEY (`FILE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_nopad_ci COMMENT ='파일';
CREATE INDEX IX_FILE_01 ON FILE.TB_FILE (SVC_CD, TBL_NM, DSTN_CLMN_VAL);