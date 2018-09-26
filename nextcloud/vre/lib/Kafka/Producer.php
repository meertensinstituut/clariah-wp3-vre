<?php

namespace OCA\Vre\Kafka;

use OCA\Vre\Log\VreLog;
use \RdKafka;

class Producer
{
    private $producer;
    private $topic;

    public function __construct($topicName) {
        if(empty($topicName)) {
            VreLog::log("Kafka producer not created because topic is empty.");
            return;
        }
        VreLog::log("Create kafka producer for topic: {$topicName}");
        $conf = new RdKafka\Conf();
        $conf->set("group.id", GROUP_ID);
        $this->producer = new RdKafka\Producer($conf);
        $this->producer->setLogLevel(LOG_DEBUG);
        $this->producer->addBrokers(BROKER_SOCKET);
        $this->topic = $this->producer->newTopic($topicName);
    }

    public function produce(String $msg) {
        VreLog::log("Produce kafka msg: {$msg}");
        $this->topic->produce(RD_KAFKA_PARTITION_UA, 0, $msg);
    }

    public function produceJson(OwnCloudKafkaMsg $msg) {
        $this->produce(json_encode($msg));
    }

}