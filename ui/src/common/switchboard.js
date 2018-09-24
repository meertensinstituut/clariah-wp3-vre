import React from "react";
import $ from "jquery";

const DOMAIN = 'http://localhost:9010/switchboard/rest';
export default class Switchboard extends React.Component {

    static getServices(objectId) {
        let url = `${DOMAIN}/object/${objectId}/services`;
        return $.get({
            url: url
        });
    }

    static async getViewers(objectId) {
        let url = `${DOMAIN}/object/${objectId}/viewers`;
        const response = await fetch(url);
        this.validate(response);
        return await response.json();
    }

    static async postDeployment(serviceName, params) {
        let url = `${DOMAIN}/exec/${serviceName}`;
        const response =  await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json; charset=utf-8"
            },
            body: JSON.stringify(params)
        });
        this.validate(response);
        return await response.json();
    }

    static getParams(serviceId) {
        let url = `${DOMAIN}/services/${serviceId}/params`;
        return $.get({
            url: url
        });
    }

    static getDeploymentStatusResult(workDir) {
        let url = `${DOMAIN}/exec/task/${workDir}`;
        return $.get({
            url: url,
        });
    }

    /**
     * Poll and wait untill requestBody.status equals deploymentStatus
     */
    static getDeploymentStatusResultWhen(workDir, deploymentStatus, deferred = new $.Deferred()) {
        const timeout = 1000;

        this.getDeploymentStatusResult(workDir).done((data, textStatus, xhr) => {
            if (deploymentStatus === data.status) {
                deferred.resolve(data);
            } else {
                setTimeout(() => {
                    this.getDeploymentStatusResultWhen(workDir, deploymentStatus, deferred)
                }, timeout);
            }
        }).fail((xhr) => {
            deferred.resolve({
                httpStatus: xhr.status,
                msg: xhr.responseJSON.msg
            });
        });
        return deferred;
    }


    static validate(response) {
        if (!response.ok) {
            console.log("response error", response);
            throw Error(response.statusText);
        }
        return response;
    }

}
