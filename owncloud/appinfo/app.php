<?php 

namespace OCA\Vre\AppInfo;

$app = new Application();
$app->getContainer()->query('VreHooks')->register();

?>