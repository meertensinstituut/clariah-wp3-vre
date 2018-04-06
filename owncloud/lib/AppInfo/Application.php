<?php

namespace OCA\Vre\AppInfo;

require_once(dirname(__FILE__) . '/config.php');

use \OCP\AppFramework\App;

use \OCA\Vre\Hooks\VreHooks;

use \OCA\Vre\Kafka\Producer;

class Application extends App
{

    /**
     * @var Producer
     */
    private $producer;

    public function __construct(array $urlParams = array()) {
        parent::__construct(APP_NAME, $urlParams);

        $container = $this->getContainer();
        $server = $container->getServer();
        $this->producer = new Producer(TOPIC_NAME);

        /**
         * Controllers
         */
        $container->registerService('VreHooks', function ($c) use ($server) {
            return new VreHooks(
                $server->getUserSession(),
                $server->getUserManager(),
                $server->getRootFolder(),
                $this->producer
            );
        });
    }
}