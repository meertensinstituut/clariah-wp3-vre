import React from "react";
import PropTypes from 'prop-types';

export default class StatePropsViewer extends React.Component {

    render() {
        if(this.props.hide) return null;

        return (
            <div>
                <div>
                    <p>state:</p>
                    <pre>{JSON.stringify(this.props.state, null, 2)}</pre>
                </div>
                <div>
                    <p>props:</p>
                    <pre>{JSON.stringify(this.props.props, null, 2)}</pre>
                </div>
            </div>
        );
    }
}

StatePropsViewer.propTypes = {
    state: PropTypes.object.isRequired,
    props: PropTypes.object.isRequired
};

StatePropsViewer.defaultTypes = {
  hide: false
};