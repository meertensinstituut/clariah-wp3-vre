import UserResource from "../user/user-resource";
import Resource from "./resource";

const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = 'objects/_table/object';
const DELETE_FILTER = {deleted: false};

export default class Dreamfactory {

    static async getObjects(params) {
        const url = `${DOMAIN}/${OBJECT}?${params}`;
        return getWithUser(url, DELETE_FILTER);
    }

    static async getObject(id) {
        const url = `${DOMAIN}/${OBJECT}/${id}`;
        const response = await get(url, DELETE_FILTER);
        return Resource.validate(response);
    }

    static async getObjectCount() {
        const url = `${DOMAIN}/objects/_table/user_file_count`;
        return await getWithUser(url);
    }
}

async function getWithUser(url, filters = {}) {
    const userData = await UserResource.whoAmI();
    const filtersWithUser = Object.assign({user_id: userData.user}, filters);
    const response = await get(url, filtersWithUser);
    return Resource.validate(response);
}

async function get(url, filters = {}) {
    return await fetch(addFilters(url, filters), {
        method: 'GET',
        headers: {'X-DreamFactory-Api-Key': `${process.env.REACT_APP_KEY_GET_OBJECTS}`}
    });
}

/**
 * Add encoded filters to url,
 * @return String url
 */
function addFilters(url, filters) {
    let filterString = "";
    let first = true;

    Object.keys(filters).forEach(function(key) {
        if(!first) {
            filterString += ' AND ';
        }
        filterString += `(${key} = '${filters[key]}')`;
        first = false;
    });

    const hasParams = (url.indexOf('?') !== -1);

    return url
        + (hasParams ? '&' : '?')
        + `filter=${filterString}`;
}
