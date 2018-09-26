<?php
namespace OCA\Vre\Log;

use OC\OCS\Exception;

class VreLog
{

    private function __construct() {
    }

    public static function log(String $msg, Exception $e = null) {
        file_put_contents(VRE_LOG, '[' . date('Y-m-d H:i:s', time()) . "] " . $msg . "\n", FILE_APPEND);
        if(!is_null($e)) {
            file_put_contents(VRE_LOG, $e->getTraceAsString() . "\n");
        }
    }


}