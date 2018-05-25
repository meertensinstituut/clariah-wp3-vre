import React from "react";
import $ from "jquery";

const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = '/objects/_table/object';
const USER = 'admin';
export default class Dreamfactory extends React.Component {

    static getObjects(params) {
        return $.get({
            url: `${DOMAIN}${OBJECT}?${params}&filter=user_id%20%3D%20${USER}`,
            beforeSend: function (xhr) {
                xhr.setRequestHeader('X-DreamFactory-Api-Key', process.env.REACT_APP_KEY_GET_OBJECTS)
            }
        });
    }

    static getObjectCount() {
        return $.get({
            url: `${DOMAIN}/objects/_table/user_file_count?filter=user_id%20%3D%20${USER}`,
            beforeSend: function (xhr) {
                xhr.setRequestHeader('X-DreamFactory-Api-Key', process.env.REACT_APP_KEY_GET_OBJECTS)
            }
        });
    }

}