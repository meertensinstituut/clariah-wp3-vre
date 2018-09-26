<?php
namespace OCA\Vre\Hooks;

use OC\Files\Filesystem;
use OCA\Vre\Kafka\OwnCloudKafkaMsg;
use \OCA\Vre\Kafka\Producer as Producer;
use OCA\Vre\Log\VreLog;

class VreHooks
{

    private $userSession;
    private $userManager;
    private $rootFolder;

    /**
     * @var Producer
     */
    private $producer;

    public function __construct(
        $userSession,
        $userManager,
        $rootFolder,
        Producer $producer
    ) {
        $this->userSession = $userSession;
        $this->userManager = $userManager;
        $this->rootFolder = $rootFolder;
        $this->producer = $producer;
    }

    public function register() {
        \OCP\Util::connectHook('OC_Filesystem', 'create', $this, 'filesystem_create_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'update', $this, 'filesystem_update_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'rename', $this, 'filesystem_rename_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'delete', $this, 'filesystem_delete_hook');
    }

    public function filesystem_create_hook($params) {
        $this->registerAction("create", $params);
    }

    public function filesystem_update_hook($params) {
        $this->registerAction("update", $params);
    }

    public function filesystem_delete_hook($params) {
        $this->registerAction("delete", $params);
    }

    public function filesystem_rename_hook($params) {
        $this->registerMoveAction("rename", $params);
    }

    private function registerMoveAction($action, $params) {
        $oldpath = $params[Filesystem::signal_param_oldpath];
        $newpath = $params[Filesystem::signal_param_newpath];
        $this->produceMsgWithOldAndNewPath($action, $oldpath, $newpath);
        $path = " from " . $oldpath . " to " . $newpath;
        $this->logActionWithPath($action, $path);
    }

    private function registerAction($action, $params) {
        $path = $params[Filesystem::signal_param_path];
        $this->produceMsgWithPath($action, $path);
        $this->logActionWithPath($action, $path);
    }

    private function logActionWithPath($action, $path) {
        $uid = $this->findUser();
        VreLog::log($uid . ":\t" . $action . " " . $path);
    }

    private function findUser(): String {
        $user = $this->userSession->getUser();
        $uid = $user ? $user->getUID() : null;
        return $uid;
    }

    private function produceMsgWithPath($action, $path) {
        $msg = OwnCloudKafkaMsg::makeWithPath($action, $this->findUser(), $path);
        $this->producer->produceJson($msg);
    }

    private function produceMsgWithOldAndNewPath($action, $oldpath, $newpath) {
        $msg = OwnCloudKafkaMsg::makeWithOldAndNewPath($action, $this->findUser(), $oldpath, $newpath);
        $this->producer->produceJson($msg);
    }

}