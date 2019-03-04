-- BOM schema

-- !Ups

CREATE TABLE `part` (
  `id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `color` varchar(100) DEFAULT NULL,
  `material` varchar(100) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- !Downs


DROP TABLE `part`;
