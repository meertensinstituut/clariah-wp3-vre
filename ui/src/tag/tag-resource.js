import {SWITCHBOARD_ENDPOINT} from "../config";
import Resource from "../common/resource";

export default class TagResource {

    static async postTag(tag) {
        let url = `${SWITCHBOARD_ENDPOINT}/tags/`;
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8"
            },
            body: JSON.stringify(tag)
        });
        return Resource.validate(response);
    }

    static async postObjectTag(objectId, tagId) {
        let url = `${SWITCHBOARD_ENDPOINT}/tags/${tagId}/objects`;
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8"
            },
            body: JSON.stringify({"object": objectId})
        });
        return Resource.validate(response);
    }

    static async deleteObjectTag(objectId, tagId) {
        let url = `${SWITCHBOARD_ENDPOINT}/tags/${tagId}/objects/${objectId}`;
        const response = await fetch(url, {
            method: 'delete',
            headers: {
                "Content-Type": "application/json; charset=utf-8"
            }
        });
        return Resource.validate(response);
    }
}