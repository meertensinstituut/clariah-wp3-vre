import UserResource from "../user/user-resource";
import Resource from "./resource";

const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = 'objects/_table/object';

export default class Dreamfactory {

    static async getObjects(params) {
        const url = `${DOMAIN}/${OBJECT}?${params}`;
        return getWithUser(url);
    }

    static async getObject(id) {
        const url = `${DOMAIN}/${OBJECT}/${id}`;
        const response = await get(url);
        return Resource.validate(response);
    }

    static async getObjectCount() {
        const url = `${DOMAIN}/objects/_table/user_file_count`;
        return await getWithUser(url);
    }
}

async function getWithUser(url) {
    const userData = await UserResource.whoAmI();
    const response = await get(addUser(url, userData.user));
    return Resource.validate(response);
}

async function get(url) {
    return await fetch(url, {
        method: 'GET',
        headers: {'X-DreamFactory-Api-Key': `${process.env.REACT_APP_KEY_GET_OBJECTS}`}
    });
}

function addUser(url, user) {
    return url
        + ((url.indexOf('?') === -1) ? '?' : '&')
        + `filter=user_id%20%3D%20${user}`;
}
