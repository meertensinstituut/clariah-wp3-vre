import $ from "jquery";
import UserResource from "../user/user-resource";

const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = 'objects/_table/object';
let user = null;

export default class Dreamfactory {

    static getObjects(params) {
        const url = `${DOMAIN}/${OBJECT}?${params}`;
        return getWithUser(url);
    }

    static getObject(id) {
        const url = `${DOMAIN}/${OBJECT}/${id}`;
        return get(url);
    }

    static getObjectCount() {
        const url = `${DOMAIN}/objects/_table/user_file_count`;
        return getWithUser(url);
    }
}

function getWithUser(url) {
    if (user) {
        return get(addUser(url));
    }
    const deferred = new $.Deferred();
    UserResource.whoAmI().done((data) => {
        user = data.user;
        get(addUser(url)).done((data) => {
            deferred.resolve(data);
        });
    });
    return deferred;
}

function get(url) {
    return $.get({
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader('X-DreamFactory-Api-Key', process.env.REACT_APP_KEY_GET_OBJECTS)
        }
    });
}

function addUser(url) {
    return url
        + ((url.indexOf('?') === -1) ? '?' : '&')
        + `filter=user_id%20%3D%20${user}`;
}
