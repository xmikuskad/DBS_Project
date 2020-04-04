DROP DATABASE IF EXISTS `dbs_db`;
CREATE DATABASE `dbs_db` charset=utf8;
USE dbs_db;

CREATE TABLE dbs_db.users (
	`ID` int NOT NULL AUTO_INCREMENT,
	`NAME` varchar(45) NOT NULL,
	`ADDRESS` varchar(45) NOT NULL,
	`PASSWORD` varchar(45) NOT NULL,
	`EMAIL` varchar(45) NOT NULL,
	`PRIVILEDGED` bool NOT NULL, 
	PRIMARY KEY (`ID`),
	UNIQUE KEY `EMAIL_UNIQUE` (`EMAIL`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.chefs (
	`ID` int NOT NULL AUTO_INCREMENT,
	`NAME` varchar(45) NOT NULL,
	PRIMARY KEY (`ID`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.food (
	`ID` int NOT NULL AUTO_INCREMENT,
	`NAME` varchar(45) NOT NULL,
	`PRICE` int NOT NULL,
	PRIMARY KEY (`ID`),
	UNIQUE KEY `NAME_UNIQUE` (`NAME`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.ingredients (
	`ID` int NOT NULL AUTO_INCREMENT,
	`NAME` varchar(45) NOT NULL, 
	PRIMARY KEY (`ID`),
	UNIQUE KEY `NAME_UNIQUE` (`NAME`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.food_chef (
	`ID` int NOT NULL AUTO_INCREMENT,
	`CHEF_ID` int NOT NULL,
	`FOOD_ID` int NOT NULL,
	PRIMARY KEY (`ID`),
	CONSTRAINT `fc_chefs` FOREIGN KEY (`CHEF_ID`) REFERENCES `chefs` (ID),
	CONSTRAINT `fc_food` FOREIGN KEY (`FOOD_ID`) REFERENCES `food` (ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.food_ingredients (
	`ID` int NOT NULL AUTO_INCREMENT,
	`FOOD_ID` int NOT NULL, 
	`INGREDIENTS_ID` int NOT NULL,
	PRIMARY KEY (`ID`),
	CONSTRAINT `fi_food` FOREIGN KEY (`FOOD_ID`) REFERENCES `food` (ID),
	CONSTRAINT `fi_ingredients` FOREIGN KEY (`INGREDIENTS_ID`) REFERENCES `ingredients` (ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.orders (
	`ID` int NOT NULL AUTO_INCREMENT,
	`TIME` timestamp NOT NULL,
	`USER_ID` int NOT NULL,
	PRIMARY KEY (`ID`),
	CONSTRAINT `o_users` FOREIGN KEY (`USER_ID`) REFERENCES `users` (ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.food_orders (
	`ID` int NOT NULL AUTO_INCREMENT,
	`FOOD_ID` int NOT NULL,
	`ORDER_ID` int NOT NULL,
	`COUNT` int NOT NULL,
	PRIMARY KEY (`ID`), 
	CONSTRAINT `fo_order` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (ID),
	CONSTRAINT `fo_food` FOREIGN KEY (`FOOD_ID`) REFERENCES `food` (ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

CREATE TABLE dbs_db.bill (
	`ID` int NOT NULL AUTO_INCREMENT,
	`ORDER_ID` int NOT NULL,
	`PAID` bool NOT NULL,
	`PRICE` int NOT NULL,
	PRIMARY KEY (`ID`),
	CONSTRAINT `b_order` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (ID)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;

INSERT INTO users (NAME, ADDRESS, PASSWORD, EMAIL, PRIVILEDGED) VALUES ("root", "-", "root", "root@aplikac.ia", 1);