import $ from "jquery";
import React from "react";

const DOMAIN = 'http://localhost:9010/switchboard/rest';
export default class Switchboard extends React.Component {

    static getServices(objectId) {
        let url = `${DOMAIN}/object/${objectId}/services`;
        return $.get({
            url: url
        });
    }

    static getParams(serviceId) {
        let url = `${DOMAIN}/services/${serviceId}/params`;
        return $.get({
            url: url
        });
    }

    static postDeployment(serviceName, config) {
        let url = `${DOMAIN}/exec/${serviceName}`;
        return $.post({
            url: url,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(config)
        });
    }

    static getDeploymentStatus(workDir) {
        let url = `${DOMAIN}/exec/task/${workDir}`;
        return $.get({
            url: url,
        });
    }

}
