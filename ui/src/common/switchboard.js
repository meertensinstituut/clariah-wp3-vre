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

    static postDeployment(serviceName, params) {
        let url = `${DOMAIN}/exec/${serviceName}`;
        return $.post({
            url: url,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(params)
        });
    }

    static getDeploymentStatus(workDir) {
        let url = `${DOMAIN}/exec/task/${workDir}`;
        return $.get({
            url: url,
        });
    }

    static getDeploymentStatusWhenStatus(workDir, status, deferred = new $.Deferred()) {
        const timeout = 1000;

        this.getDeploymentStatus(workDir).done((data, textStatus, xhr) => {
            if (status === data.status) {
                deferred.resolve(data);
            } else {
                setTimeout(() => this.getDeploymentStatusWhenStatus(workDir, status, deferred), timeout);
            }
        }).fail((xhr) => {
            deferred.resolve({
                httpStatus: xhr.status,
                msg: xhr.responseJSON.msg
            });
        });
        return deferred;
    }

}
