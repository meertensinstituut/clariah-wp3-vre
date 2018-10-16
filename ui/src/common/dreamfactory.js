import UserResource from "../user/user-resource";
import Resource from "./resource";

const DELETE_FILTER = {deleted: false};
const DOMAIN = 'http://localhost:8089/api/v2';
const OBJECT = 'objects/_table/object';
const TAG = 'objects/_table/tag';

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

    static async searchTags(namesLike) {
        const userData = await UserResource.whoAmI();
        const filters = {
            'owner': userData.user,
            'name': {'value': namesLike, 'operator': 'LIKE'}
        };
        const url = `${DOMAIN}/${TAG}`;
        const response = await get(url, filters);
        return Resource.validate(response);
    }

    static async getObjectCount() {
        const url = `${DOMAIN}/objects/_table/user_file_count`;
        return await getWithUser(url);
    }

    static async getObjectTags(ids) {
        const url = `${DOMAIN}/objects/_table/object_full_tag?object=${ids.join(',')}`;
        return Resource.validate(await get(url));
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
 * Add encoded dreamfactory filters to url
 *
 * Example filters-object:
 * {
 *   'name': 'john', // compares using '='
 *   'lastname': {value: 'd', 'operator': 'like'},
 * }
 * Result:
 * {url}?filter=(name=john)AND(lastname%20like%d%25)
 *
 * Supported operators:
 * - '='
 * - 'LIKE'
 *
 * @return String url
 */
function addFilters(url, filters) {
    let filterString = "";
    let first = true;

    Object.keys(filters).forEach(function (key) {
        if (!first) {
            filterString += ' AND ';
        }
        const filter = filters[key];
        if(filter.operator === 'LIKE') {
            filterString += `(${key} like ${filter.value}%25)`;
        } else if (
            filter.operator === '='
            ||
            filter.operator === undefined
        ) {
            filterString += `(${key} = '${filter}')`;
        } else {
            throw new Error("operator not supported: " + filter.operator);
        }
        first = false;
    });

    const hasParams = (url.indexOf('?') !== -1);

    return url
        + (hasParams ? '&' : '?')
        + `filter=${filterString}`;
}
