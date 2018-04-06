<?php
namespace OCA\Vre\Kafka;
require_once(dirname(dirname(__FILE__)) . '/AppInfo/config.php');

use OCA\VRE\AppInfo\Config;

use \RdKafka;

class ExampleProducer {
    public static function produce() {
        $consumerConf = new RdKafka\Conf();
        $consumerConf->set("group.id", GROUP_ID);

        $producer = new RdKafka\Producer($consumerConf);
        $producer->setLogLevel(LOG_DEBUG);
        $producer->addBrokers(BROKER_SOCKET);

        $topic = $producer->newTopic(TOPIC_NAME);
        $topic->produce(RD_KAFKA_PARTITION_UA, 0, "Kafka o kafka waar bent u?");
    }
}

ExampleProducer::produce();