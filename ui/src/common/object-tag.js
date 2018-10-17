import React from "react";
import PropTypes from 'prop-types';

export default class ObjectTag extends React.Component {

    constructor(props) {
        super(props);
        this.state = {}
    }

    handleDelete = () => {
        this.props.onDelete(this.props.objectTag);
    };

    render() {
        const objectTag = this.props.objectTag;
        return (
            <span className="label label-primary tag">
                {objectTag.type}:{objectTag.name}
                &nbsp;
                <i className="fa fa-times clickable" area-hidden="true" onClick={this.handleDelete}/>
            </span>
        );
    }
}

ObjectTag.propTypes = {
    objectTag: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired
};
