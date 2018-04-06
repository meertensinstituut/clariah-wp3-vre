<?php
namespace OCA\Vre\Hooks;

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
        \OCP\Util::connectHook('OC_Filesystem', 'rename', $this, 'filesystem_rename_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'post_rename', $this, 'filesystem_post_rename_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'create', $this, 'filesystem_create_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'post_create', $this, 'filesystem_post_create_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'copy', $this, 'filesystem_copy_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'post_copy', $this, 'filesystem_post_copy_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'write', $this, 'filesystem_write_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'post_write', $this, 'filesystem_post_write_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'update', $this, 'filesystem_update_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'post_update', $this, 'filesystem_post_update_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'read', $this, 'filesystem_read_hook');
        \OCP\Util::connectHook('OC_Filesystem', 'delete', $this, 'filesystem_delete_hook');
    }

    public function filesystem_post_create_hook($params) {
        $this->registerAction("create", $params);
    }

    public function filesystem_post_update_hook($params) {
        $this->registerAction("update", $params);
    }

    public function filesystem_post_rename_hook($params) {
        $this->registerAction("rename", $params);
    }

    public function filesystem_post_copy_hook($params) {
        $this->registerAction("copy", $params);
    }

    public function filesystem_delete_hook($params) {
        $this->registerAction("delete", $params);
    }

    private function registerAction($action, $params) {
        $path = $params[\OC\Files\Filesystem::signal_param_path];
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

}