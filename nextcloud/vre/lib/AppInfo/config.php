<?php
define("APP_NAME", getenv("NEXTCLOUD_APP_NAME"));
define("GROUP_ID", getenv("NEXTCLOUD_GROUP_NAME"));
define("TOPIC_NAME", getenv("NEXTCLOUD_TOPIC_NAME"));
define("BROKER_SOCKET", "kafka:" . getenv("KAFKA_PORT"));
define("VRE_LOG", "/var/www/html/data/vrelog.txt");
