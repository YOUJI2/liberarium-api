-- 스키마 선택
CREATE DATABASE IF NOT EXISTS bookdb
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE bookdb;

-- 도서 기본 정보 테이블
CREATE TABLE IF NOT EXISTS book (
  isbn            VARCHAR(20)  NOT NULL,  -- 도서 고유 번호
  title           VARCHAR(300) NOT NULL,  -- 제목
  subtitle        VARCHAR(300) NULL,      -- 부제목
  author          VARCHAR(200) NOT NULL,  -- 작가 (추후 author 테이블로 분리 고려)
  image_url       VARCHAR(500) NULL,
  published_date  DATE         NULL,      -- 출판 일자
  publisher       VARCHAR(200) NULL,      -- 출판사 (추후 publisher 분리 고려)
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (isbn),
  KEY idx_book_title (title),   -- 제목 기준
  KEY idx_book_author (author), -- 작가 기준 검색시
  KEY idx_book_published_date (published_date) -- 최신일자 기준
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 도서 상세 테이블 (1:1 관계)
CREATE TABLE IF NOT EXISTS book_detail (
  isbn                VARCHAR(20)    NOT NULL,           -- isbn값 기본키
  sold_out          TINYINT(1)     NOT NULL DEFAULT 0, -- 품절 상태
  free_shipping     TINYINT(1)     NOT NULL DEFAULT 0, -- 무료 배송
  list_price        INT UNSIGNED   NULL,          -- 정가(원)
  sale_price        INT UNSIGNED   NULL,          -- 판매가(원)
  author_bio        TEXT           NULL,        -- 작가의 소개글
  book_description  MEDIUMTEXT     NULL,        -- 책 소개글
  created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (isbn),
  CONSTRAINT fk_book_detail_book FOREIGN KEY (isbn) REFERENCES book(isbn)
  ON DELETE CASCADE ON UPDATE RESTRICT
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 분류 카탈로그 (대략 6~8개 내외의 고정으로)
CREATE TABLE IF NOT EXISTS catalog (
  id          INT NOT NULL AUTO_INCREMENT,
  code        VARCHAR(100)  NOT NULL UNIQUE,  -- 영문 코드
  name        VARCHAR(100) NOT NULL,          -- 카탈로그 이름
  sort_order  TINYINT UNSIGNED NOT NULL DEFAULT 0, -- 고정 노출 순서
  active      TINYINT(1)   NOT NULL DEFAULT 1,     -- 현재 활성된 카탈로그인지
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_catalog_active (active, sort_order) -- 활성화된 것만 빠르게 노출
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 책-카탈로그 매핑 (N:N 관계)
CREATE TABLE IF NOT EXISTS book_catalog (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  isbn        VARCHAR(20)      NOT NULL,           -- 길이 book.isbn과 통일
  catalog_id  INT NOT NULL,
  created_at  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_book_catalog__book
  FOREIGN KEY (isbn) REFERENCES book(isbn)
  ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_book_catalog__catalog
  FOREIGN KEY (catalog_id) REFERENCES catalog(id)
  ON DELETE RESTRICT ON UPDATE RESTRICT,
  UNIQUE KEY uk_book_catalog_isbn_catalog (isbn, catalog_id), -- 중복 방지
  KEY idx_book_catalog_catalog (catalog_id),
  KEY idx_book_catalog_isbn (isbn)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

