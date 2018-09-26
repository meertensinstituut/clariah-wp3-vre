<?php
namespace OCA\Vre\Kafka;
require_once(dirname(dirname(__FILE__)) . '/AppInfo/config.php');

use OCA\VRE\AppInfo\Config;

use \RdKafka;

class ExampleConsumer {
    public static function consume() {
        $consumerConf = new RdKafka\Conf();
        $consumerConf->set("group.id", GROUP_ID);
        $consumer = new RdKafka\Consumer($consumerConf);
        $consumer->setLogLevel(LOG_DEBUG);
        $consumer->addBrokers(BROKER_SOCKET);

        $topicConf = new RdKafka\TopicConf();
        $topicConf->set("auto.commit.interval.ms", 1e3);
        $topicConf->set("offset.store.sync.interval.ms", 60e3);
        $topic = $consumer->newTopic(TOPIC_NAME, $topicConf);

        $topic->consumeStart(0, RD_KAFKA_OFFSET_STORED);

        while (true) {
            $timeoutMs = 1000;
            $msg = $topic->consume(0, $timeoutMs);
            if (!empty($msg->err)) {
                echo $msg->errstr() . "\n";
            } else if($msg->payload) {
                echo $msg->payload, "\n";
            }
        }

    }
}

ExampleConsumer::consume();