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
                {path: '/files', name: 'Files'},
                {path: '/react-gui', name: 'React GUI'}
            ]
        }
    }

    render() {
        return (
            <ul className='nav'>
                {this.state.links.map((link, i) => {
                    return (
                        <li
                            className='nav-item'
                            key={i}
                        >
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