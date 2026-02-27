-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: booking_app_db
-- ------------------------------------------------------
-- Server version	8.0.35

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `accounts`
--

DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts` (
  `id` varchar(255) NOT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','CUSTOMER','HOTEL_MANAGER','STAFF') NOT NULL,
  `status` bit(1) NOT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKk8h1bgqoplx0rkngj01pm1rgp` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accounts`
--

LOCK TABLES `accounts` WRITE;
/*!40000 ALTER TABLE `accounts` DISABLE KEYS */;
INSERT INTO `accounts` VALUES ('08717750-9b8b-477a-9615-89a927fa4280','2026-01-09 21:53:42.525000','2026-01-09 21:53:42.525000','$2a$10$ArOSDPyB249ZQvp4OkGgk.ITX1tElgYmXojD8sLS1wtRvi1EdXYYG','HOTEL_MANAGER',_binary '','hotel2'),('2a664c04-8f07-45f1-b38b-4383af218423','2025-12-29 22:37:43.529000','2025-12-29 22:37:43.529000','$2a$10$8oFTn3a2.oBoo4rVLIK7jefX94Ov1qrQYUpO5/hG1tBdH8WKPXisW','CUSTOMER',_binary '','user03'),('2be453d8-1451-4f68-9d6d-7e2a2e3c439b','2025-12-29 21:50:59.638000','2025-12-29 21:50:59.638000','$2a$10$8ARMm1jj.Dmnww7EF79eg.Zac0.7Bfnsj8e0gWMRWI2eM4PZ0MY4a','STAFF',_binary '','staff1'),('56cdecd7-8f37-445b-8328-f15c7cc0916a','2026-01-04 09:55:00.358000','2026-01-04 09:55:00.358000','$2a$10$fg1C1JCroIQUywmnDZaegOgOyJTfmrUwun8S6KAZAQYuxt757ExUS','STAFF',_binary '\0','staff4'),('5dcd2a35-25f3-4008-8b7f-26f74658c54c','2026-01-04 09:40:13.467000','2026-01-04 09:40:13.467000','$2a$10$maGNo/XxOgATwZp30r9pLuhh37uGbEoBCfwHR1/j9NrO39NCxfSs.','STAFF',_binary '\0','staff3'),('77d003aa-92a2-4e54-a536-bb8a65245976','2026-01-09 21:58:17.104000','2026-01-09 21:58:17.104000','$2a$10$Zv0BoxFJ8mAqlLd17hv3Xe2ll4Hrf7d.RjqRzHXR2za3oNntfpE3C','HOTEL_MANAGER',_binary '','hotel3'),('8d129336-df35-4644-9a5e-86bf74208207','2025-12-29 22:19:02.660000','2025-12-29 22:19:02.660000','$2a$10$mBJ/zZVTxULdCbkPMLwUe.caQjCj7I9leHRHli0/119jH/13s18.e','CUSTOMER',_binary '','user02'),('a1d50dc5-e3e2-4e81-a896-04845eb07ed9','2025-12-28 10:14:13.690000','2025-12-28 10:14:13.690000','$2a$10$WkYFg/sk/4cwUXutdrQmXuMeVpIRhna744nUdMXC.YEDBdMwl2Zku','ADMIN',_binary '','admin123'),('a9b374e9-cf9f-45bd-82b7-28eda27c86df','2025-12-28 10:26:14.622000','2025-12-28 10:26:14.622000','$2a$10$E.MNKz6A5eIR4.Qo9Hw0dubgNqUqk1nKgwaLtG97vZfE.A2UDXWFe','CUSTOMER',_binary '','user01'),('bb92716a-5265-4766-9d75-9a988caf819c','2026-01-02 13:06:57.413000','2026-01-02 13:06:57.413000','$2a$10$5YST/hFZJjxuS6zTk342juijeZfFMDtQizTCFd2QEMPP.0PcMtg.q','STAFF',_binary '\0','staff2'),('d89fbb57-c86c-44e0-8c22-77085731aa73','2025-12-28 22:14:08.138000','2025-12-28 22:14:08.138000','$2a$10$vR.wI23rVsOTukTjT1GbC.kv9Cak6FBvnzUpD.2mFbULzBDbW0G2m','HOTEL_MANAGER',_binary '','hotel1');
/*!40000 ALTER TABLE `accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `amenity`
--

DROP TABLE IF EXISTS `amenity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `amenity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `common` bit(1) NOT NULL,
  `description` text,
  `name` varchar(255) NOT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `hotel_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfxd577jef0yesk2uoafyvwixd` (`hotel_id`),
  CONSTRAINT `FKfxd577jef0yesk2uoafyvwixd` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `amenity`
--

LOCK TABLES `amenity` WRITE;
/*!40000 ALTER TABLE `amenity` DISABLE KEYS */;
INSERT INTO `amenity` VALUES (1,_binary '',_binary '','mô tả chi tiết tiện ích 2','tiện ích 2','2026-01-04 21:08:14.973000','2026-01-04 21:09:57.894000',1),(3,_binary '',_binary '\0','mô tả tiện ích 3','tiện ích 3','2026-01-04 21:22:08.580000','2026-01-04 21:22:08.580000',1),(4,_binary '',_binary '','mô tả chi tiết tiện ích 1','tiện ích 1','2026-01-04 21:59:03.572000','2026-01-04 21:59:27.411000',1),(6,_binary '',_binary '\0','mô tả tiện ích riêng của phòng 101','tiện ích riêng của phòng 101','2026-01-08 19:54:53.338000','2026-01-08 19:54:53.338000',1);
/*!40000 ALTER TABLE `amenity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `booking_rooms`
--

DROP TABLE IF EXISTS `booking_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK71qgxnmri4s08xrlny5wptrej` (`booking_id`),
  KEY `FKcjk0abrppkbsw5w03uq8tvgfc` (`room_id`),
  CONSTRAINT `FK71qgxnmri4s08xrlny5wptrej` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FKcjk0abrppkbsw5w03uq8tvgfc` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `booking_rooms`
--

LOCK TABLES `booking_rooms` WRITE;
/*!40000 ALTER TABLE `booking_rooms` DISABLE KEYS */;
INSERT INTO `booking_rooms` VALUES (3,8,1),(4,9,1),(5,10,1),(6,10,2),(9,13,2),(10,14,2),(11,15,2),(12,16,2),(13,17,1),(14,18,1),(15,19,2),(16,20,1),(17,21,1),(27,31,1),(28,32,1),(29,33,2),(30,34,1),(31,35,2);
/*!40000 ALTER TABLE `booking_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `booking_services`
--

DROP TABLE IF EXISTS `booking_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_services` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `total` decimal(12,0) NOT NULL,
  `booking_id` bigint NOT NULL,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1etky587qu1tqlr3t1r7w59gx` (`booking_id`),
  KEY `FKhhofk6n050slfqp0v6e65axk3` (`service_id`),
  CONSTRAINT `FK1etky587qu1tqlr3t1r7w59gx` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FKhhofk6n050slfqp0v6e65axk3` FOREIGN KEY (`service_id`) REFERENCES `services` (`id`),
  CONSTRAINT `booking_services_chk_1` CHECK ((`quantity` >= 1))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `booking_services`
--

LOCK TABLES `booking_services` WRITE;
/*!40000 ALTER TABLE `booking_services` DISABLE KEYS */;
INSERT INTO `booking_services` VALUES (2,1,0,8,1),(3,1,0,9,1),(4,1,0,10,1),(7,1,0,13,1),(8,1,0,14,1),(9,1,0,15,1),(10,1,0,16,1),(11,1,0,17,1),(12,1,0,18,1),(13,1,0,19,1),(14,1,0,20,1),(15,1,0,21,1),(17,1,0,31,1);
/*!40000 ALTER TABLE `booking_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bookings`
--

DROP TABLE IF EXISTS `bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_status` enum('CANCELED','COMPLETED','CONFIRMED','PAYING','PENDING') NOT NULL,
  `checkin_date` date NOT NULL,
  `checkout_date` date NOT NULL,
  `note` text,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `payment_method` enum('BANK_TRANSFER','CASH','VN_PAY') NOT NULL,
  `total_amount` decimal(12,0) NOT NULL,
  `customer_id` bigint NOT NULL,
  `hotel_id` bigint NOT NULL,
  `payment_status` enum('PAID','PENDING','UNPAID') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbvfibgflhsb0g2hnjauiv5khs` (`customer_id`),
  KEY `FK7y09f5lun38jnooaw2hch0ke9` (`hotel_id`),
  CONSTRAINT `FK7y09f5lun38jnooaw2hch0ke9` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`),
  CONSTRAINT `FKbvfibgflhsb0g2hnjauiv5khs` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bookings`
--

LOCK TABLES `bookings` WRITE;
/*!40000 ALTER TABLE `bookings` DISABLE KEYS */;
INSERT INTO `bookings` VALUES (8,'CANCELED','2026-02-01','2026-02-02','string','2026-01-04 17:58:55.292000','2026-01-13 21:07:03.338000','CASH',0,1,1,'PAID'),(9,'COMPLETED','2026-02-01','2026-02-02','ghi chú đặt phòng','2026-01-12 11:12:50.146000','2026-01-13 21:09:29.889000','CASH',87432,1,1,'PAID'),(10,'COMPLETED','2026-02-01','2026-02-02','ghi chú đặt phòng','2026-01-12 19:39:12.335000','2026-01-12 19:39:12.335000','CASH',65765,1,1,'PAID'),(13,'COMPLETED','2026-02-01','2026-02-02','ghi chú đặt phòng','2026-01-12 19:55:24.636000','2026-01-12 19:55:24.636000','CASH',456546,1,1,'PAID'),(14,'CONFIRMED','2026-02-01','2026-02-02','ghi chú đặt phòng','2026-01-13 20:20:35.619000','2026-01-26 19:40:04.796000','CASH',0,1,1,'PAID'),(15,'CONFIRMED','2026-02-01','2026-02-02','ghi chú đặt phòng','2026-01-13 20:23:16.473000','2026-01-19 22:04:58.392000','CASH',0,1,1,'PAID'),(16,'PENDING','2026-02-01','2026-02-02','ghi chú đặt phòng','2026-01-13 20:25:02.614000','2026-01-13 20:25:02.614000','CASH',0,4,1,'PAID'),(17,'COMPLETED','2026-02-10','2026-02-12','ghi chú đặt phòng','2026-01-26 20:32:21.328000','2026-01-26 20:32:21.328000','CASH',0,4,1,'PAID'),(18,'PENDING','2026-02-10','2026-02-15','ghi chú đặt phòng','2026-01-26 20:36:37.241000','2026-01-26 20:36:37.241000','CASH',0,4,1,'PAID'),(19,'PENDING','2026-02-20','2026-02-25','ghi chú đặt phòng','2026-01-26 20:36:38.885000','2026-01-26 20:36:38.885000','CASH',0,4,1,'PAID'),(20,'PENDING','2026-02-22','2026-02-23','ghi chú đặt phòng','2026-01-26 21:19:02.309000','2026-01-26 21:19:02.309000','CASH',0,4,1,'PAID'),(21,'COMPLETED','2026-03-22','2026-03-23','ghi chú đặt phòng','2026-01-29 18:55:09.459000','2026-01-29 18:55:09.459000','CASH',645564,4,1,'PAID'),(31,'PENDING','2026-03-22','2026-03-23','ghi chú đặt phòng','2026-02-03 21:12:09.909000','2026-02-03 21:12:09.909000','VN_PAY',0,4,1,'PAID'),(32,'PENDING','2026-02-26','2026-02-27','fecf','2026-02-03 21:12:15.967000','2026-02-03 21:12:15.967000','VN_PAY',645564,1,1,'PAID'),(33,'PENDING','2026-02-03','2026-02-04',NULL,'2026-02-03 21:13:27.808000','2026-02-03 21:13:27.808000','CASH',646464,1,1,'PAID'),(34,'PENDING','2026-02-03','2026-02-04',NULL,'2026-02-03 21:13:42.401000','2026-02-03 21:13:42.401000','VN_PAY',645564,1,1,'PAID'),(35,'PENDING','2026-02-05','2026-02-06',NULL,'2026-02-03 21:15:05.403000','2026-02-03 21:15:05.403000','VN_PAY',646464,1,1,'PAID');
/*!40000 ALTER TABLE `bookings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `hometown` varchar(255) DEFAULT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `path_image` varchar(255) DEFAULT NULL,
  `phone_number` varchar(10) NOT NULL,
  `account_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4njtl3pvfduamug24b9qmpy0x` (`account_id`),
  CONSTRAINT `FKor0fx9fttvasr4grtaqnltyrl` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
INSERT INTO `customers` VALUES (1,'Phạm Xuân Văn','nam','Thái Bình','2025-12-29 21:18:08.735000','2025-12-29 21:18:08.735000','','0387463712','a9b374e9-cf9f-45bd-82b7-28eda27c86df'),(4,'Phạm Xuân Văn','nam','Thái Bình','2025-12-30 20:09:33.073000','2025-12-30 20:09:33.073000','','0387463712','8d129336-df35-4644-9a5e-86bf74208207');
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employees`
--

DROP TABLE IF EXISTS `employees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_of_birth` date NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `gender` varchar(255) NOT NULL,
  `hometown` varchar(255) NOT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `path_image` varchar(255) DEFAULT NULL,
  `phone_number` varchar(10) NOT NULL,
  `salary` decimal(12,0) NOT NULL,
  `account_id` varchar(255) NOT NULL,
  `hotel_id` bigint NOT NULL,
  `position_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKguw5r3c5brob94evptgkjl9hr` (`account_id`),
  KEY `FKl6mewl85djruoxecubfwwn07u` (`hotel_id`),
  KEY `FKngcpgx7fx5kednw3m7u0u8of3` (`position_id`),
  CONSTRAINT `FK8dwhb0qmor08fl06pde1bef3c` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `FKl6mewl85djruoxecubfwwn07u` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`),
  CONSTRAINT `FKngcpgx7fx5kednw3m7u0u8of3` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employees`
--

LOCK TABLES `employees` WRITE;
/*!40000 ALTER TABLE `employees` DISABLE KEYS */;
INSERT INTO `employees` VALUES (1,'2025-01-01','Phạm Xuân Văn','nam','Hà Nội','2025-12-29 21:52:09.450000','2025-12-29 21:52:09.450000','','0394875612',10000000,'2be453d8-1451-4f68-9d6d-7e2a2e3c439b',1,6),(2,'2025-01-01','Nguyễn Văn A','nam','Hà Nội','2026-01-02 13:06:57.436000','2026-02-04 13:53:45.175000','','0394875612',10000000,'bb92716a-5265-4766-9d75-9a988caf819c',1,6);
/*!40000 ALTER TABLE `employees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `feedbacks`
--

DROP TABLE IF EXISTS `feedbacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedbacks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comments` text,
  `rating` int NOT NULL,
  `review_date` date NOT NULL,
  `booking_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqikxfingr5pq4lidl8gsd3ubb` (`booking_id`,`customer_id`),
  KEY `FKi9b9keigxngo4a35fgwt4h2v6` (`customer_id`),
  CONSTRAINT `FKi9b9keigxngo4a35fgwt4h2v6` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKqnenmceqm1fre2qcuv9fbdw2d` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `feedbacks_chk_1` CHECK (((`rating` <= 5) and (`rating` >= 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feedbacks`
--

LOCK TABLES `feedbacks` WRITE;
/*!40000 ALTER TABLE `feedbacks` DISABLE KEYS */;
/*!40000 ALTER TABLE `feedbacks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hotel_images`
--

DROP TABLE IF EXISTS `hotel_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hotel_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `path_image` varchar(255) NOT NULL,
  `hotel_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3a7s8tfu8scbjepghkb5k4rud` (`path_image`),
  KEY `FKrj3n45f8oqy1yr996g14j757i` (`hotel_id`),
  CONSTRAINT `FKrj3n45f8oqy1yr996g14j757i` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hotel_images`
--

LOCK TABLES `hotel_images` WRITE;
/*!40000 ALTER TABLE `hotel_images` DISABLE KEYS */;
INSERT INTO `hotel_images` VALUES (1,'/uploads/hotels/1767971260672_image.png',2),(2,'/uploads/hotels/1767971260705_image1.png',2),(3,'/uploads/hotels/1767971260733_image2.png',2),(4,'/uploads/hotels/1767971260741_image3.png',2),(5,'/uploads/hotels/1767971260757_image4.png',2),(6,'/uploads/hotels/1767971350519_image.png',3),(7,'/uploads/hotels/1767971350537_image1.png',3),(8,'/uploads/hotels/1767971350558_image2.png',3),(9,'/uploads/hotels/1767971350568_image3.png',3),(10,'/uploads/hotels/1767971350585_image4.png',3),(11,'/uploads/hotels/1768184281291_image.png',1),(12,'/uploads/hotels/1768184281316_image1.png',1),(13,'/uploads/hotels/1768184281337_image2.png',1),(14,'/uploads/hotels/1768184281351_image3.png',1),(15,'/uploads/hotels/1768192346195_JPEG_20260112_113218_8735122731700527939.jpg',1),(16,'/uploads/hotels/1768192346211_JPEG_20260112_113218_9189468144783393949.jpg',1),(17,'/uploads/hotels/1768203172236_JPEG_20260112_143240_6640700642990547455.jpg',1);
/*!40000 ALTER TABLE `hotel_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hotels`
--

DROP TABLE IF EXISTS `hotels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hotels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `address` varchar(255) NOT NULL,
  `category` varchar(255) NOT NULL,
  `description` text,
  `name` varchar(255) NOT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `path_image` varchar(255) DEFAULT NULL,
  `phone` varchar(10) NOT NULL,
  `rating` double NOT NULL,
  `account_id` varchar(255) NOT NULL,
  `status` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9chtb8f43n4ex7cfqk6nksnnr` (`account_id`),
  CONSTRAINT `FK646upacfjdthhvu8aina4hi7q` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `hotels_chk_1` CHECK ((`status` between 0 and 2))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hotels`
--

LOCK TABLES `hotels` WRITE;
/*!40000 ALTER TABLE `hotels` DISABLE KEYS */;
INSERT INTO `hotels` VALUES (1,_binary '','Khu Cửa Đại, TP. Hội An, Quảng Nam','HOLIDAY','Khách sạn nghỉ dưỡng gần biển Cửa Đại.','Mường Thanh Holiday Hội An','2025-12-28 22:14:21.507000','2025-12-28 22:14:21.507000','/uploads/hotels/1767971350568_image3.png','0235392992',0,'d89fbb57-c86c-44e0-8c22-77085731aa73',NULL),(2,_binary '','Khu Cửa Đại, TP. Hội An, Quảng Nam','Luxury','Khách sạn nghỉ dưỡng gần biển Cửa Đại.','Mường Thanh Holiday Hội An','2026-01-09 21:53:56.351000','2026-01-09 21:57:07.541000','/uploads/hotels/1767971260672_image.png','0235392992',0,'08717750-9b8b-477a-9615-89a927fa4280',NULL),(3,_binary '','Lô CC2 Khu Bắc Linh Đàm, phường Định Công, thành phố Hà Nội, Việt Nam','Grand','Khách sạn 4 sao khu Bắc Linh Đàm, có hệ thống phòng họp và dịch vụ thư giãn như spa/bể bơi/karaoke.','Mường Thanh Grand Hà Nội','2026-01-09 21:58:38.094000','2026-01-09 21:58:38.094000','/uploads/hotels/1767971350558_image2.png','0243640868',0,'77d003aa-92a2-4e54-a536-bb8a65245976',NULL);
/*!40000 ALTER TABLE `hotels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invalidated_tokens`
--

DROP TABLE IF EXISTS `invalidated_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invalidated_tokens` (
  `id` varchar(255) NOT NULL,
  `expiry_time` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invalidated_tokens`
--

LOCK TABLES `invalidated_tokens` WRITE;
/*!40000 ALTER TABLE `invalidated_tokens` DISABLE KEYS */;
INSERT INTO `invalidated_tokens` VALUES ('02ccee65-914a-4c93-8b35-a809e65a6fa3','2026-01-20 20:43:12.000000'),('07a6148c-952f-4768-b178-8cfc7538c67c','2025-12-31 21:56:50.000000'),('0ae334a5-1339-446e-a4e6-c3e6ff9afa88','2025-12-29 23:15:31.000000'),('0c57ead6-52c3-4e16-b524-62ac23a63678','2026-01-21 19:43:32.000000'),('0d7d0f97-4cc7-4b2d-8550-8862aa28aa53','2026-02-02 21:35:45.000000'),('0def0d51-f9c7-4faf-b217-ad9ff6d29e77','2026-02-04 19:17:15.000000'),('14dee8f3-e7e6-42ca-b0a4-9aa1a93bf503','2026-01-15 19:34:50.000000'),('17e4cf05-1ad8-4161-a312-a77c67c3989b','2025-12-31 21:28:22.000000'),('1b44d016-7f40-4463-b554-cf38a532b250','2026-02-03 20:56:08.000000'),('36205afb-d9d7-4c10-8d3f-843c85e3a33f','2026-01-31 20:21:16.000000'),('3c76903c-e319-4687-939e-b7f2834fefdf','2025-12-31 21:21:16.000000'),('3d4b74dc-df8e-4bfc-bd56-27b3194ef33e','2025-12-31 21:59:28.000000'),('3e9c7ab5-903a-4898-b87a-486adb10bb2c','2026-01-11 18:35:20.000000'),('3efef890-adb9-430a-84dc-959f6697e8bd','2026-01-31 04:53:50.000000'),('3f009b98-7dcf-4bff-8d75-a8f8d9c93291','2025-12-30 22:17:32.000000'),('4050b211-3e1f-42ca-ab90-c293b107381b','2025-12-30 21:35:59.000000'),('419c0e39-16de-4e81-9872-b31a113fd7c6','2026-01-16 22:41:32.000000'),('44b45006-201f-4302-bd7b-4b3965d35897','2026-02-11 13:52:32.000000'),('4ca737e7-9733-426b-ac51-808b8ca6c678','2026-01-04 22:05:33.000000'),('4e57c105-a582-456f-bf2a-bd526654728e','2025-12-30 22:15:45.000000'),('50e7159c-65f3-4d43-872a-fe00cb093423','2026-02-02 21:33:10.000000'),('54102900-fd18-48af-bc90-683ed962df79','2026-02-10 20:58:21.000000'),('545ecca1-a742-49ca-b805-88feeae4b31d','2026-01-04 22:14:29.000000'),('54f559d5-dd4a-411b-94ce-a2ed72624845','2026-01-12 13:54:08.000000'),('5732c63a-1f20-4550-888a-cc03253188ba','2026-01-19 13:03:30.000000'),('590f8908-5a4a-4297-bdde-9b63a47dbcfa','2025-12-31 22:01:00.000000'),('5f17fc7f-a1a1-4f9c-9e8d-e9b422465152','2026-01-18 00:56:54.000000'),('60a6f281-e0a5-4575-b794-c0c37f92fdba','2026-01-15 20:45:23.000000'),('60f73626-c2fd-4e6b-a869-7c85cdf7cbec','2026-02-02 21:34:07.000000'),('61ae98b3-481a-4ca3-bad7-f5401b93dbb9','2026-01-12 13:47:48.000000'),('63a79018-5314-4973-bd5f-b5e698a816f0','2026-02-02 21:36:30.000000'),('64e673f1-4a27-4975-805f-50d25df4f3d4','2026-01-21 19:15:16.000000'),('68237a0f-9d79-4fe1-8512-1f5d6e068435','2025-12-30 22:14:17.000000'),('69d73ff3-f6b6-48a7-ad91-9027c11b7022','2026-01-04 22:59:50.000000'),('6b2dfe09-222f-433e-9520-35e63bea634f','2026-01-12 13:50:39.000000'),('6c6bac9c-cdc4-4a23-a894-df3a6553e896','2026-02-07 10:35:26.000000'),('6daac1a0-cdfb-4e23-9c73-6e1ac2373644','2026-02-03 20:55:31.000000'),('70a71607-9eaf-446b-932f-b9d781a5e0a4','2025-12-31 21:58:09.000000'),('71f1173f-f11d-4bfa-9b8d-29564c54896d','2026-02-05 18:54:53.000000'),('737122cd-5d7b-450d-8c0f-937849763583','2026-02-07 14:18:03.000000'),('7522c456-7f2d-41e8-b9c0-3204c925da4e','2025-12-31 21:54:54.000000'),('7695aab7-6050-438a-8b89-f7fba78c79fd','2026-02-03 20:55:57.000000'),('76c6e036-5020-4842-90f4-42b8d19583f8','2026-01-31 20:08:27.000000'),('78e9d9be-f4be-4ee4-9415-db496eba3edc','2026-01-02 13:26:43.000000'),('7965a730-3ae3-4a61-9dbc-48df6dbbd294','2026-01-29 05:14:34.000000'),('7f7a3197-96da-46f2-b6d2-4562ad146aa6','2025-12-31 22:10:14.000000'),('819ce674-7907-4621-88ab-f59035656a1a','2026-01-04 21:43:28.000000'),('8adb11cb-b172-4e9c-855d-f441bb861cce','2026-02-03 20:55:43.000000'),('8ba97623-5598-480f-975e-5bfc70c000e1','2026-02-03 20:55:52.000000'),('8d4b2a5c-291b-45cc-8697-dc13b6de60f5','2026-02-02 21:33:42.000000'),('8e7886c4-fc6a-4b03-9073-d948b8b740c8','2026-01-19 19:02:07.000000'),('8e98cfc1-a641-4eb2-ae6c-16eb9641a909','2026-01-26 21:33:31.000000'),('8eac8554-b65d-427e-8246-17e26fcba4dc','2026-02-10 20:57:49.000000'),('92911409-7f72-4ab6-9d74-96a0e92f7179','2026-01-17 20:28:20.000000'),('935423ec-3c5f-4036-a380-19be8102c7b2','2026-01-19 11:11:00.000000'),('98f3829b-eedd-40c1-b9ae-20d19dacfcf7','2026-01-08 20:31:46.000000'),('9b0702bf-4e49-4ec1-872d-1a2259b33493','2026-01-12 15:17:44.000000'),('9f564900-8cd2-4069-b94d-256fc4f1bd0f','2026-01-08 21:39:15.000000'),('a95d76a4-6d1a-4226-ae23-c0d545e0b489','2026-01-06 21:46:17.000000'),('b1b2dfc7-8ba2-4458-89d4-10b0d7fa1f73','2026-01-11 22:01:48.000000'),('b260ca70-edb1-4e2f-ab93-1cf9c75c8662','2025-12-29 23:17:18.000000'),('b3f161f4-5050-4a27-bb60-0e42d99b39c9','2026-01-02 13:30:49.000000'),('b7d4b138-5355-4683-b50c-22d4611861d3','2026-01-20 20:08:45.000000'),('ba8ccbb4-5331-4476-8b51-181eeeec1e2b','2026-01-12 10:43:28.000000'),('bad133aa-82de-4a6a-b1e8-66d39b7c329e','2025-12-30 21:26:59.000000'),('bbabb9a5-cee9-4cb4-8fb6-54bda0659fd3','2026-01-12 11:33:18.000000'),('be992119-4614-4b6e-a68b-2e832731a572','2026-01-12 15:31:49.000000'),('bec11bf2-e681-4197-b10a-a7b810737531','2026-01-02 13:31:05.000000'),('c1894fed-25b9-4ad7-b58a-49669a2c2433','2025-12-30 23:49:49.000000'),('c608c8bd-2aa9-4571-b2af-38886d94abd3','2026-02-04 19:42:14.000000'),('c724fc60-2139-44e1-b159-7f5d5ef8a83b','2025-12-31 22:04:14.000000'),('c982a8c3-17fc-44fb-b05c-95365a322bfb','2025-12-30 21:29:28.000000'),('cead96f6-4f49-43e5-84fc-c9a619f8b11d','2026-01-19 14:32:31.000000'),('d0512003-04ec-4553-8cb2-b7ae2ef4fb22','2026-01-11 11:47:39.000000'),('d38aa9a7-3179-4571-80db-8e9cb6fd5e25','2026-01-31 20:32:08.000000'),('d3f832f5-dc71-4bc7-b326-d760e1b6bfc1','2026-01-12 13:49:42.000000'),('d56b44b5-875e-4a0f-80c1-c088540743e1','2025-12-30 22:10:46.000000'),('d6a611fd-15ac-4d3f-bdd4-57ff0e4c0a80','2026-01-19 09:38:05.000000'),('d75ac0cb-b20f-4d10-9695-56aed39233e7','2026-01-02 13:29:45.000000'),('d78ec161-7860-4f6b-8d9e-0f83c037e7f6','2026-01-19 19:58:31.000000'),('d7d4cd0c-4404-4800-8ee6-de77c2ab6218','2026-02-03 20:56:55.000000'),('dd458a3d-9b87-4609-9dc8-f4ec425c86c0','2026-02-06 18:55:00.000000'),('ddf6b4d7-e532-41e6-ab94-e666bf4bd18b','2026-01-19 14:16:54.000000'),('e40cfc4d-b277-4714-9d3e-02a47ee39dc9','2026-01-09 23:41:18.000000'),('f0d7ca38-1452-4dcb-ae13-2133f70a46c9','2025-12-28 21:53:41.000000'),('f124ab14-8c90-4e17-87cb-cc18a2a1c709','2026-01-19 10:37:44.000000'),('f1cdc06d-ce04-4aa6-b1bb-7606289680b2','2025-12-30 00:09:01.000000'),('f716e46b-794e-41c6-b68d-f9d79ed208b3','2026-02-04 23:52:18.000000'),('f7c27b04-b86e-408e-9620-685fd4c04018','2026-01-09 12:34:45.000000'),('ffc4d638-d0ef-4730-bfa1-3a5d770ce980','2026-01-12 10:15:42.000000');
/*!40000 ALTER TABLE `invalidated_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payrolls`
--

DROP TABLE IF EXISTS `payrolls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payrolls` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `month` date DEFAULT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PAID','REJECTED') NOT NULL,
  `total_salary` decimal(12,0) NOT NULL,
  `employee_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKiyfp8uysuhgfkod3xcdhjm7qf` (`employee_id`),
  CONSTRAINT `FKiyfp8uysuhgfkod3xcdhjm7qf` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payrolls`
--

LOCK TABLES `payrolls` WRITE;
/*!40000 ALTER TABLE `payrolls` DISABLE KEYS */;
/*!40000 ALTER TABLE `payrolls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `positions`
--

DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `positions`
--

LOCK TABLES `positions` WRITE;
/*!40000 ALTER TABLE `positions` DISABLE KEYS */;
INSERT INTO `positions` VALUES (6,'mô tả công việc lê tân','lê tân'),(7,'mo ta cong viec ve sinh phong','ve sinh phong');
/*!40000 ALTER TABLE `positions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `reporting_month` date NOT NULL,
  `total_booking` int NOT NULL,
  `total_revenue` decimal(15,0) NOT NULL,
  `total_service` int NOT NULL,
  `hotel_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKawbu2vda5qljn76pbvirw1wuv` (`hotel_id`),
  CONSTRAINT `FKawbu2vda5qljn76pbvirw1wuv` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`),
  CONSTRAINT `reports_chk_1` CHECK ((`total_booking` >= 0)),
  CONSTRAINT `reports_chk_2` CHECK ((`total_service` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reports`
--

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
/*!40000 ALTER TABLE `reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_amenity`
--

DROP TABLE IF EXISTS `room_amenity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_amenity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantity` int NOT NULL,
  `amenity_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKaj52mcu9d5c3dglfx9f07qi94` (`amenity_id`),
  KEY `FKtgr6n91m36o9yajpnoec6488n` (`room_id`),
  CONSTRAINT `FKaj52mcu9d5c3dglfx9f07qi94` FOREIGN KEY (`amenity_id`) REFERENCES `amenity` (`id`),
  CONSTRAINT `FKtgr6n91m36o9yajpnoec6488n` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `room_amenity_chk_1` CHECK ((`quantity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_amenity`
--

LOCK TABLES `room_amenity` WRITE;
/*!40000 ALTER TABLE `room_amenity` DISABLE KEYS */;
INSERT INTO `room_amenity` VALUES (1,1,3,1),(2,1,6,1);
/*!40000 ALTER TABLE `room_amenity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_images`
--

DROP TABLE IF EXISTS `room_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `path_image` varchar(255) NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe27op0sx2om5coycg8awxhmp8` (`path_image`),
  KEY `FKtky1jnwoh1hv50m263p2vlt0y` (`room_id`),
  CONSTRAINT `FKtky1jnwoh1hv50m263p2vlt0y` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_images`
--

LOCK TABLES `room_images` WRITE;
/*!40000 ALTER TABLE `room_images` DISABLE KEYS */;
INSERT INTO `room_images` VALUES (1,'/uploads/hotels/1768065799287_image.png',1),(2,'/uploads/hotels/1768065799302_image1.png',1),(3,'/uploads/hotels/1768065799323_image2.png',1),(4,'/uploads/hotels/1768065799333_image3.png',1),(5,'/uploads/hotels/1768065799349_image4.png',1),(6,'/uploads/hotels/1768186460830_image.png',1),(7,'/uploads/hotels/1768186495225_image.png',1),(8,'/uploads/hotels/1768186495244_image1.png',1),(9,'/uploads/hotels/1768186495259_image2.png',1),(10,'/uploads/hotels/1768188769319_JPEG_20260112_103248_5223034752952123319.jpg',1),(14,'/uploads/hotels/1768204144196_JPEG_20260112_144901_6971406754573994333.jpg',1),(15,'/uploads/hotels/1768204144215_JPEG_20260112_144902_4482528102960833183.jpg',1);
/*!40000 ALTER TABLE `room_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_types`
--

DROP TABLE IF EXISTS `room_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_types`
--

LOCK TABLES `room_types` WRITE;
/*!40000 ALTER TABLE `room_types` DISABLE KEYS */;
INSERT INTO `room_types` VALUES (1,'mô tả phòng bình dân','bình dân'),(2,'mô tả phòng cao cấp','cao cấp');
/*!40000 ALTER TABLE `room_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capacity` int NOT NULL,
  `description` text,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `price` decimal(12,0) NOT NULL,
  `room_number` text NOT NULL,
  `status` enum('AVAILABLE','BOOKED','MAINTENANCE','OCCUPIED') NOT NULL,
  `hotel_id` bigint NOT NULL,
  `room_type_id` bigint NOT NULL,
  `path_image` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKp5lufxy0ghq53ugm93hdc941k` (`hotel_id`),
  KEY `FKh9m2n1paq5hmd3u0klfl7wsfv` (`room_type_id`),
  CONSTRAINT `FKh9m2n1paq5hmd3u0klfl7wsfv` FOREIGN KEY (`room_type_id`) REFERENCES `room_types` (`id`),
  CONSTRAINT `FKp5lufxy0ghq53ugm93hdc941k` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (1,2,'bsbshsjsjs','2025-12-28 22:15:07.506000','2025-12-28 22:15:07.506000',645564,'101','AVAILABLE',1,2,'/uploads/hotels/1768065799302_image1.png'),(2,2,'mô tả phòng 102\n','2026-01-08 19:34:02.222000','2026-01-08 19:34:02.222000',646464,'102','AVAILABLE',1,2,'/uploads/hotels/1768065799333_image3.png');
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `services`
--

DROP TABLE IF EXISTS `services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `services` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` varchar(255) DEFAULT NULL,
  `on_create` datetime(6) DEFAULT NULL,
  `on_update` datetime(6) DEFAULT NULL,
  `unit_price` decimal(12,0) NOT NULL,
  `hotel_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrw2afcn1g75sa7tffbf0ae483` (`hotel_id`),
  CONSTRAINT `FKrw2afcn1g75sa7tffbf0ae483` FOREIGN KEY (`hotel_id`) REFERENCES `hotels` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `services`
--

LOCK TABLES `services` WRITE;
/*!40000 ALTER TABLE `services` DISABLE KEYS */;
INSERT INTO `services` VALUES (1,'đơn giá theo ngày','thuê xe','2025-12-28 22:17:42.755000','2025-12-28 22:17:42.755000',180000,1);
/*!40000 ALTER TABLE `services` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-28  0:10:37
