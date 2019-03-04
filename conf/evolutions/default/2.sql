-- BOM schema

-- !Ups

CREATE TABLE `assembly_part` (
  `assembly_id` varchar(36) NOT NULL,
  `part_id` varchar(36) NOT NULL,
  UNIQUE KEY `assembly_part_assembly_id_IDX` (`assembly_id`,`part_id`) USING BTREE,
  KEY `assembly_part_part_FK` (`assembly_id`),
  KEY `assembly_part_part_FK_1` (`part_id`),
  CONSTRAINT `assembly_part_part_FK` FOREIGN KEY (`assembly_id`) REFERENCES `part` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `assembly_part_part_FK_1` FOREIGN KEY (`part_id`) REFERENCES `part` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- !Downs

DROP TABLE `assembly_part`;