import React from "react";
import {NavLink} from "react-router-dom";
import 'bootstrap/dist/css/bootstrap.min.css';
import './navigation.css';

export default class Navigation extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            links: [
                {path: '/', name: 'Home'},
                {path: '/files', name: 'Files'}
            ]
        }
    }

    render() {
        return (
            <ul className='nav'>
                {this.state.links.map((link) => {
                    return (
                        <li className='nav-item'>
                            <NavLink
                                exact
                                to={link.path}
                                className='nav-link'
                                activeClassName='active'>{link.name}</NavLink>
                        </li>
                    );
                })}
            </ul>
        );
    }
}