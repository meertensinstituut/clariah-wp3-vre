import Resource from "./resource";
import {SWITCHBOARD_ENDPOINT} from "../config";

export default class Switchboard {

    static async getServices(objectId) {
        let url = `${SWITCHBOARD_ENDPOINT}/object/${objectId}/services`;
        const response = await fetch(url);
        return Resource.validate(response);
    }

    static async getViewers(objectId) {
        let url = `${SWITCHBOARD_ENDPOINT}/object/${objectId}/viewers`;
        const response = await fetch(url);
        return Resource.validate(response);
    }

    static async postDeployment(serviceName, params) {
        let url = `${SWITCHBOARD_ENDPOINT}/exec/${serviceName}`;
        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json; charset=utf-8"
            },
            body: JSON.stringify(params)
        });
        return Resource.validate(response);
    }

    static async getParams(serviceId) {
        let url = `${SWITCHBOARD_ENDPOINT}/services/${serviceId}/params`;
        const response = await fetch(url);
        return Resource.validate(response);
    }

    static async getDeploymentStatusResult(workDir) {
        let url = `${SWITCHBOARD_ENDPOINT}/exec/task/${workDir}`;
        const response = await fetch(url);
        return Resource.validate(response);
    }

    /**
     * Poll and wait untill requestBody.status equals deploymentStatus
     */
    static async getDeploymentWhen(workDir, deploymentStatus) {
        const timeout = 1000;
        const response = await this.getDeploymentStatusResult(workDir);
        if (deploymentStatus === response.status) {
            return response;
        } else {
            await this.wait(timeout);
            return await this.getDeploymentWhen(workDir, deploymentStatus);
        }
    }

    static wait = async (ms) => new Promise((r)=>setTimeout(r, ms));

}
