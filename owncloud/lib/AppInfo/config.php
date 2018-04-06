<?php
define("APP_NAME", getenv("OWNCLOUD_APP_NAME"));
define("GROUP_ID", getenv("OWNCLOUD_GROUP_NAME"));
define("TOPIC_NAME", getenv("OWNCLOUD_TOPIC_NAME"));
define("BROKER_SOCKET", "kafka:" . getenv("KAFKA_PORT"));
define("VRE_LOG", "/var/www/html/data/vrelog.txt");
