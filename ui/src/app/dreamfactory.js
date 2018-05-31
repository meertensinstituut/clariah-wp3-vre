import React from "react";
import $ from "jquery";

const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = '/objects/_table/object';
const USER = 'admin';
export default class Dreamfactory extends React.Component {

    static getObjects(params) {
        const url = `${DOMAIN}${OBJECT}?${params}&filter=user_id%20%3D%20${USER}`;
        return get(url);
    }

    static getObjectCount() {
        const url = `${DOMAIN}/objects/_table/user_file_count?filter=user_id%20%3D%20${USER}`;
        return get(url);
    }
}

function get(url) {
    return $.get({
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader('X-DreamFactory-Api-Key', process.env.REACT_APP_KEY_GET_OBJECTS)
        }
    });

}