<?php
namespace OCA\Vre\Kafka;

class OwnCloudKafkaMsg
{
    /**
     * @var String
     */
    public $action;

    /**
     * @var String
     */
    public $user;

    /**
     * @var string
     */
    public $userPath;

    /**
     * @var int
     */
    public $timestamp;

    public static function makeWithPath($action, $user, $path) : OwnCloudKafkaMsg {
        $msg = self::setCommonFields($action, $user, $path);
        return $msg;
    }

    public static function makeWithOldAndNewPath($action, $user, $oldPath, $newPath) : OwnCloudKafkaMsg {
        $msg = self::setCommonFields($action, $user, $newPath);
        $msg->newPath = $newPath;
        $msg->oldPath = $oldPath;
        return $msg;
    }

    /**
     * @param $action
     * @param $user
     * @param $path
     * @return OwnCloudKafkaMsg
     */
    private static function setCommonFields($action, $user, $path): OwnCloudKafkaMsg {
        $msg = new OwnCloudKafkaMsg();
        $msg->action = $action;
        $msg->user = $user;
        $msg->userPath = self::createUserPath($user, $path);
        $msg->timestamp = time();
        return $msg;
    }

    /**
     * @param $user
     * @param $path
     * @return string
     */
    private static function createUserPath($user, $path) {
        return $user . "/files/" . $path;
    }


}